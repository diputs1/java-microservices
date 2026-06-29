package com.tungdt.microservices.ordering.outbox;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import java.time.Instant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRelayService {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate,
            MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEventEntity event : outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING.name())) {
            publish(event);
        }
    }

    @Transactional
    public int resetFailedEvents() {
        var failedEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name());
        failedEvents.forEach(OutboxEventEntity::resetForRetry);
        return failedEvents.size();
    }

    private void publish(OutboxEventEntity event) {
        try {
            rabbitTemplate.convertAndSend(event.getExchangeName(), event.getRoutingKey(), event.getPayload(),
                    message -> {
                        String traceId = MDC.get("traceId");
                        if (traceId != null) {
                            message.getMessageProperties().setHeader("X-Trace-Id", traceId);
                        }
                        message.getMessageProperties().setHeader("eventId", event.getEventId().toString());
                        message.getMessageProperties().setHeader("eventType", event.getEventType());
                        return message;
                    });
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            counter("published", event).increment();
            log.info("Published outbox event eventId={} type={}", event.getEventId(), event.getEventType());
        } catch (RuntimeException ex) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLastError(limit(ex.getMessage()));
            if (attempts >= MAX_ATTEMPTS) {
                event.setStatus(OutboxEventStatus.FAILED);
            }
            counter("failed", event).increment();
            log.warn("Cannot publish outbox event eventId={} attempts={}", event.getEventId(), attempts, ex);
        }
    }

    private io.micrometer.core.instrument.Counter counter(String result, OutboxEventEntity event) {
        return meterRegistry.counter("outbox.publish",
                Tags.of("result", result, "eventType", event.getEventType(), "routingKey", event.getRoutingKey()));
    }

    private String limit(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
