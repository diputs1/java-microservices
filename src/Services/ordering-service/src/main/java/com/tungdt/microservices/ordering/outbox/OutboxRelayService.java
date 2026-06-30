package com.tungdt.microservices.ordering.outbox;

import com.tungdt.microservices.common.web.TraceHeaders;
import com.tungdt.microservices.common.web.TraceIdFilter;
import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OutboxRelayService {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;
    private static final Duration CLAIM_TIMEOUT = Duration.ofMinutes(5);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            MeterRegistry meterRegistry,
            PlatformTransactionManager transactionManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay:5000}")
    public void publishPendingEvents() {
        for (OutboxEventEntity event : claimEvents()) {
            publish(event);
        }
    }

    @Transactional
    public int resetFailedEvents() {
        var failedEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name());
        failedEvents.forEach(OutboxEventEntity::resetForRetry);
        return failedEvents.size();
    }

    private List<OutboxEventEntity> claimEvents() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Instant staleBefore = now.minus(CLAIM_TIMEOUT);
            List<OutboxEventEntity> events = outboxEventRepository.findClaimableForUpdate(BATCH_SIZE, staleBefore);
            for (OutboxEventEntity event : events) {
                event.setStatus(OutboxEventStatus.IN_PROGRESS);
                event.setClaimedAt(now);
            }
            outboxEventRepository.saveAll(events);
            outboxEventRepository.flush();
            return events;
        });
    }

    private void publish(OutboxEventEntity event) {
        try {
            rabbitTemplate.convertAndSend(event.getExchangeName(), event.getRoutingKey(), event.getPayload(),
                    message -> {
                        MessageProperties properties = message.getMessageProperties();
                        String eventId = event.getEventId().toString();
                        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
                        properties.setMessageId(eventId);
                        properties.setCorrelationId(eventId);
                        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        properties.setHeader("eventId", eventId);
                        properties.setHeader("eventType", event.getEventType());
                        if (traceId != null) {
                            properties.setHeader(TraceHeaders.TRACE_ID, traceId);
                        }
                        return message;
                    });
            markPublished(event);
            counter("published", event).increment();
            log.info("Published outbox event eventId={} type={}", event.getEventId(), event.getEventType());
        } catch (RuntimeException ex) {
            int attempts = markFailedOrPendingRetry(event, ex);
            counter("failed", event).increment();
            log.warn("Cannot publish outbox event eventId={} attempts={}", event.getEventId(), attempts, ex);
        }
    }

    private void markPublished(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> {
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setClaimedAt(null);
            event.setLastError(null);
            outboxEventRepository.save(event);
        });
    }

    private int markFailedOrPendingRetry(OutboxEventEntity event, RuntimeException ex) {
        return transactionTemplate.execute(status -> {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setClaimedAt(null);
            event.setLastError(limit(ex.getMessage()));
            event.setStatus(attempts >= MAX_ATTEMPTS ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING);
            outboxEventRepository.save(event);
            return attempts;
        });
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
