package com.tungdt.microservices.ordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.ordering.dto.OutboxRetryResponse;
import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.outbox.OrderOutboxService;
import com.tungdt.microservices.ordering.outbox.OutboxRelayService;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxOperationsServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxRelayService outboxRelayService;

    private OutboxOperationsService outboxOperationsService;

    @BeforeEach
    void setUp() {
        outboxOperationsService = new OutboxOperationsService(outboxEventRepository, outboxRelayService);
    }

    @Test
    void getFailedEventsMapsRepositoryResults() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventType("ORDER_CREATED");
        event.setAggregateType("ORDER");
        event.setAggregateId("42");
        event.setExchangeName(OrderOutboxService.EVENTS_EXCHANGE);
        event.setRoutingKey(OrderOutboxService.ORDER_CREATED_ROUTING_KEY);
        event.setStatus(OutboxEventStatus.FAILED);
        event.setAttempts(5);
        event.setLastError("rabbit unavailable");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name()))
                .thenReturn(List.of(event));

        var response = outboxOperationsService.getFailedEvents();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).eventType()).isEqualTo("ORDER_CREATED");
        assertThat(response.get(0).status()).isEqualTo(OutboxEventStatus.FAILED.name());
    }

    @Test
    void retryFailedEventsResetsThenPublishesPendingEvents() {
        when(outboxRelayService.resetFailedEvents()).thenReturn(2);

        OutboxRetryResponse response = outboxOperationsService.retryFailedEvents();

        assertThat(response.resetCount()).isEqualTo(2);
        verify(outboxRelayService).publishPendingEvents();
    }
}
