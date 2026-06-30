package com.tungdt.microservices.ordering.outbox;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics implements MeterBinder {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxMetrics(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (OutboxEventStatus status : OutboxEventStatus.values()) {
            Gauge.builder("outbox.events", outboxEventRepository,
                            repository -> repository.countByStatus(status.name()))
                    .description("Number of outbox events by status")
                    .tag("status", status.name())
                    .register(registry);
        }

        Gauge.builder("outbox.lag.seconds", this, OutboxMetrics::pendingLagSeconds)
                .description("Age in seconds of the oldest pending outbox event")
                .register(registry);
    }

    double pendingLagSeconds() {
        return outboxEventRepository.findFirstByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING.name())
                .map(OutboxEventEntity::getCreatedAt)
                .map(createdAt -> Duration.between(createdAt, Instant.now()).toSeconds())
                .orElse(0L)
                .doubleValue();
    }
}
