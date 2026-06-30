package com.tungdt.microservices.ordering.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxRelayService = new OutboxRelayService(
                outboxEventRepository,
                rabbitTemplate,
                new SimpleMeterRegistry(),
                new NoOpTransactionManager()
        );
    }

    @Test
    void resetFailedEventsMovesEventsBackToPending() {
        OutboxEventEntity event = outboxEvent(5);
        event.setStatus(OutboxEventStatus.FAILED);
        event.setLastError("rabbit unavailable");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name()))
                .thenReturn(List.of(event));

        int resetCount = outboxRelayService.resetFailedEvents();

        assertThat(resetCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING.name());
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getLastError()).isNull();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publishPendingEventsAddsStableMessageMetadata() throws Exception {
        OutboxEventEntity event = outboxEvent(0);
        when(outboxEventRepository.findClaimableForUpdate(eq(50), any()))
                .thenReturn(List.of(event));

        outboxRelayService.publishPendingEvents();

        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq("microservices.events"),
                eq("order.created"),
                any(Object.class),
                postProcessorCaptor.capture()
        );

        MessageProperties properties = new MessageProperties();
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        postProcessorCaptor.getValue().postProcessMessage(message);

        String eventId = event.getEventId().toString();
        assertThat(properties.getMessageId()).isEqualTo(eventId);
        assertThat(properties.getCorrelationId()).isEqualTo(eventId);
        assertThat(properties.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(properties.getHeaders())
                .containsEntry("eventId", eventId)
                .containsEntry("eventType", "ORDER_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED.name());
        assertThat(event.getClaimedAt()).isNull();
    }

    @Test
    void publishPendingEventsReturnsEventToPendingWhenPublishFailsBelowMaxAttempts() {
        OutboxEventEntity event = outboxEvent(1);
        when(outboxEventRepository.findClaimableForUpdate(eq(50), any()))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("rabbitmq unavailable")).when(rabbitTemplate)
                .convertAndSend(eq("microservices.events"), eq("order.created"), any(Object.class),
                        any(MessagePostProcessor.class));

        outboxRelayService.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING.name());
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getLastError()).isEqualTo("rabbitmq unavailable");
    }

    @Test
    void publishPendingEventsMarksFailedWhenMaxAttemptsReached() {
        OutboxEventEntity event = outboxEvent(4);
        when(outboxEventRepository.findClaimableForUpdate(eq(50), any()))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("rabbitmq unavailable")).when(rabbitTemplate)
                .convertAndSend(eq("microservices.events"), eq("order.created"), any(Object.class),
                        any(MessagePostProcessor.class));

        outboxRelayService.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED.name());
        assertThat(event.getAttempts()).isEqualTo(5);
        assertThat(event.getClaimedAt()).isNull();
    }

    private OutboxEventEntity outboxEvent(int attempts) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setEventType("ORDER_CREATED");
        event.setAggregateType("ORDER");
        event.setAggregateId("100");
        event.setExchangeName("microservices.events");
        event.setRoutingKey("order.created");
        event.setPayload("{\"eventType\":\"ORDER_CREATED\"}");
        event.setAttempts(attempts);
        return event;
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
