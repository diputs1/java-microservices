package com.tungdt.microservices.ordering.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxMetricsTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxMetrics outboxMetrics;

    @BeforeEach
    void setUp() {
        outboxMetrics = new OutboxMetrics(outboxEventRepository);
    }

    @Test
    void pendingLagSecondsUsesOldestPendingEventAge() {
        OutboxEventEntity event = new OutboxEventEntity();
        ReflectionTestUtils.setField(event, "createdAt", Instant.now().minusSeconds(30));
        when(outboxEventRepository.findFirstByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING.name()))
                .thenReturn(Optional.of(event));

        assertThat(outboxMetrics.pendingLagSeconds()).isGreaterThanOrEqualTo(29);
    }

    @Test
    void pendingLagSecondsReturnsZeroWhenThereAreNoPendingEvents() {
        when(outboxEventRepository.findFirstByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING.name()))
                .thenReturn(Optional.empty());

        assertThat(outboxMetrics.pendingLagSeconds()).isZero();
    }
}
