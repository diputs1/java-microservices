package com.tungdt.microservices.ordering.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxRelayService = new OutboxRelayService(outboxEventRepository, rabbitTemplate, new SimpleMeterRegistry());
    }

    @Test
    void resetFailedEventsMovesEventsBackToPending() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setStatus(OutboxEventStatus.FAILED);
        event.setAttempts(5);
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
}
