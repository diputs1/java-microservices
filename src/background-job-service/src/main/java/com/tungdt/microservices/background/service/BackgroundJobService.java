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
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
    public void handleOrderCreated(String payload,
            @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
            @Header(name = TraceHeaders.TRACE_ID, required = false) String traceId) {
        saveEvent("ORDER_CREATED", routingKey, traceId, payload);
    }

    @RabbitListener(queues = "email.requested")
    public void handleEmailRequested(String payload,
            @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
            @Header(name = TraceHeaders.TRACE_ID, required = false) String traceId) {
        saveEvent("EMAIL_REQUESTED", routingKey, traceId, payload);
    }

    public List<JobEventResponse> getAll() {
        return jobEventRepository.findAll().stream().map(this::toResponse).toList();
    }

    private void saveEvent(String fallbackType, String routingKey, String traceIdHeader, String payload) {
        EventEnvelope envelope = parseEnvelope(payload);
        String eventId = envelope == null || envelope.eventId() == null ? null : envelope.eventId().toString();
        String traceId = StringUtils.hasText(traceIdHeader)
                ? traceIdHeader
                : envelope == null ? null : envelope.traceId();

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
        if (StringUtils.hasText(eventId) && jobEventRepository.existsByEventId(eventId)) {
            counter("duplicate", routingKey, fallbackType).increment();
            log.info("Skip duplicate job event eventId={} routingKey={}", eventId, routingKey);
            return;
        }

        String type = envelope == null || !StringUtils.hasText(envelope.eventType())
                ? fallbackType
                : envelope.eventType();
        log.info("Receive job event type={} eventId={} routingKey={}", type, eventId, routingKey);
        JobEventEntity event = new JobEventEntity();
        event.setEventId(eventId);
        event.setTraceId(traceId);
        event.setType(type);
        event.setRoutingKey(routingKey);
        event.setPayload(payload);
        event.setReceivedAt(Instant.now());
        jobEventRepository.save(event);
        counter("stored", routingKey, type).increment();
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
