package com.tungdt.microservices.background.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.background.dto.JobEventResponse;
import com.tungdt.microservices.background.entity.JobEventEntity;
import com.tungdt.microservices.background.messaging.EventEnvelope;
import com.tungdt.microservices.background.repository.JobEventRepository;
import com.tungdt.microservices.common.web.TraceHeaders;
import com.tungdt.microservices.common.web.TraceIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BackgroundJobService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundJobService.class);
    private final JobEventRepository jobEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public BackgroundJobService(JobEventRepository jobEventRepository, ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.jobEventRepository = jobEventRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = "order.created")
    public void handleOrderCreated(Message message) {
        saveEvent("ORDER_CREATED", message);
    }

    @RabbitListener(queues = "email.requested")
    public void handleEmailRequested(Message message) {
        saveEvent("EMAIL_REQUESTED", message);
    }

    public List<JobEventResponse> getAll() {
        return jobEventRepository.findAll().stream().map(this::toResponse).toList();
    }

    private void saveEvent(String fallbackType, Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        EventEnvelope envelope = parseEnvelope(payload);
        String eventId = resolveEventId(message, envelope);
        String traceId = resolveTraceId(message, envelope);

        if (StringUtils.hasText(traceId)) {
            MDC.put(TraceIdFilter.MDC_KEY, traceId);
        }
        try {
            saveEventWithTrace(fallbackType, routingKey, payload, envelope, eventId, traceId);
        } finally {
            if (StringUtils.hasText(traceId)) {
                MDC.remove(TraceIdFilter.MDC_KEY);
            }
        }
    }

    private void saveEventWithTrace(String fallbackType, String routingKey, String payload, EventEnvelope envelope,
            String eventId, String traceId) {
        String type = envelope == null || !StringUtils.hasText(envelope.eventType())
                ? fallbackType
                : envelope.eventType();

        if (StringUtils.hasText(eventId) && jobEventRepository.existsByEventId(eventId)) {
            counter("duplicate", routingKey, type).increment();
            log.info("Skip duplicate job event eventId={} routingKey={}", eventId, routingKey);
            return;
        }

        log.info("Receive job event type={} eventId={} routingKey={}", type, eventId, routingKey);
        JobEventEntity event = new JobEventEntity();
        if (StringUtils.hasText(eventId)) {
            event.setEventId(eventId);
        }
        event.setTraceId(traceId);
        event.setType(type);
        event.setRoutingKey(routingKey);
        event.setPayload(payload);
        event.setReceivedAt(Instant.now());

        try {
            jobEventRepository.save(event);
            counter("stored", routingKey, type).increment();
        } catch (DuplicateKeyException ex) {
            counter("duplicate", routingKey, type).increment();
            log.info("Skip duplicate job event eventId={} routingKey={}", eventId, routingKey);
        }
    }

    private EventEnvelope parseEnvelope(String payload) {
        try {
            EventEnvelope envelope = objectMapper.readValue(payload, EventEnvelope.class);
            if (envelope.eventId() == null && !StringUtils.hasText(envelope.eventType())) {
                return null;
            }
            return envelope;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String resolveEventId(Message message, EventEnvelope envelope) {
        Object eventIdHeader = message.getMessageProperties().getHeaders().get("eventId");
        if (eventIdHeader != null && StringUtils.hasText(eventIdHeader.toString())) {
            return eventIdHeader.toString();
        }

        String messageId = message.getMessageProperties().getMessageId();
        if (StringUtils.hasText(messageId)) {
            return messageId;
        }

        return envelope == null || envelope.eventId() == null ? null : envelope.eventId().toString();
    }

    private String resolveTraceId(Message message, EventEnvelope envelope) {
        Object traceIdHeader = message.getMessageProperties().getHeaders().get(TraceHeaders.TRACE_ID);
        if (traceIdHeader != null && StringUtils.hasText(traceIdHeader.toString())) {
            return traceIdHeader.toString();
        }
        return envelope == null ? null : envelope.traceId();
    }

    private JobEventResponse toResponse(JobEventEntity event) {
        return new JobEventResponse(
                event.getId(),
                event.getEventId(),
                event.getTraceId(),
                event.getType(),
                event.getRoutingKey(),
                event.getPayload(),
                event.getReceivedAt()
        );
    }

    private io.micrometer.core.instrument.Counter counter(String result, String routingKey, String eventType) {
        return meterRegistry.counter("job.event.consume",
                Tags.of("result", result, "routingKey", valueOrUnknown(routingKey), "eventType", eventType));
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }
}
