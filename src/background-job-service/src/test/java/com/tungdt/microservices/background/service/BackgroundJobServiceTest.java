package com.tungdt.microservices.background.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.background.entity.JobEventEntity;
import com.tungdt.microservices.background.repository.JobEventRepository;
import com.tungdt.microservices.common.web.TraceHeaders;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServiceTest {
    @Mock
    private JobEventRepository jobEventRepository;

    private BackgroundJobService backgroundJobService;

    @BeforeEach
    void setUp() {
        backgroundJobService = new BackgroundJobService(
                jobEventRepository,
                new ObjectMapper().findAndRegisterModules(),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void handleOrderCreatedStoresEnvelopeEventIdAndType() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String payload = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventType": "ORDER_CREATED",
                  "version": 1,
                  "aggregateType": "ORDER",
                  "aggregateId": "42",
                  "occurredAt": "2026-01-01T00:00:00Z",
                  "traceId": "trace-123",
                  "data": {"orderId": 42}
                }
                """;

        backgroundJobService.handleOrderCreated(message(payload, "order.created", null, null));

        ArgumentCaptor<JobEventEntity> eventCaptor = ArgumentCaptor.forClass(JobEventEntity.class);
        verify(jobEventRepository).save(eventCaptor.capture());
        JobEventEntity event = eventCaptor.getValue();
        assertThat(event.getEventId()).isEqualTo(eventId.toString());
        assertThat(event.getTraceId()).isEqualTo("trace-123");
        assertThat(event.getType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getRoutingKey()).isEqualTo("order.created");
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    void handleOrderCreatedSkipsDuplicateEnvelopeEvent() {
        String eventId = "22222222-2222-2222-2222-222222222222";
        String payload = """
                {
                  "eventId": "22222222-2222-2222-2222-222222222222",
                  "eventType": "ORDER_CREATED",
                  "version": 1,
                  "aggregateType": "ORDER",
                  "aggregateId": "43",
                  "occurredAt": "2026-01-01T00:00:00Z",
                  "traceId": "trace-duplicate",
                  "data": {"orderId": 43}
                }
                """;
        when(jobEventRepository.existsByEventId(eventId)).thenReturn(true);

        backgroundJobService.handleOrderCreated(message(payload, "order.created", null, null));

        verify(jobEventRepository, never()).save(any());
    }

    @Test
    void handleEmailRequestedStoresLegacyPayloadWithoutEventId() {
        String payload = "{\"to\":\"customer@example.com\",\"subject\":\"Hi\"}";

        backgroundJobService.handleEmailRequested(message(payload, "email.requested", null, "trace-email"));

        ArgumentCaptor<JobEventEntity> eventCaptor = ArgumentCaptor.forClass(JobEventEntity.class);
        verify(jobEventRepository).save(eventCaptor.capture());
        JobEventEntity event = eventCaptor.getValue();
        assertThat(event.getEventId()).isNull();
        assertThat(event.getTraceId()).isEqualTo("trace-email");
        assertThat(event.getType()).isEqualTo("EMAIL_REQUESTED");
        assertThat(event.getRoutingKey()).isEqualTo("email.requested");
    }

    @Test
    void handleEmailRequestedUsesMessageIdForLegacyPayloadIdempotency() {
        String payload = "{\"to\":\"customer@example.com\",\"subject\":\"Hi\"}";

        backgroundJobService.handleEmailRequested(message(payload, "email.requested", "message-1", null));

        ArgumentCaptor<JobEventEntity> eventCaptor = ArgumentCaptor.forClass(JobEventEntity.class);
        verify(jobEventRepository).save(eventCaptor.capture());
        JobEventEntity event = eventCaptor.getValue();
        assertThat(event.getEventId()).isEqualTo("message-1");
        assertThat(event.getType()).isEqualTo("EMAIL_REQUESTED");
    }

    private Message message(String payload, String routingKey, String messageId, String traceId) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        if (messageId != null) {
            properties.setMessageId(messageId);
            properties.setHeader("eventId", messageId);
        }
        if (traceId != null) {
            properties.setHeader(TraceHeaders.TRACE_ID, traceId);
        }
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }
}
