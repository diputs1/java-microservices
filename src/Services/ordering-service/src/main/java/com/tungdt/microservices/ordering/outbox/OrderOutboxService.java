package com.tungdt.microservices.ordering.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.common.web.TraceIdFilter;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderOutboxService {
    public static final String EVENTS_EXCHANGE = "microservices.events";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void saveOrderCreated(OrderEntity order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
        OutboxEventEnvelope envelope = new OutboxEventEnvelope(
                eventId,
                "ORDER_CREATED",
                1,
                "ORDER",
                String.valueOf(order.getId()),
                occurredAt,
                MDC.get(TraceIdFilter.MDC_KEY),
                event
        );

        try {
            OutboxEventEntity outboxEvent = new OutboxEventEntity();
            outboxEvent.setEventId(eventId);
            outboxEvent.setEventType("ORDER_CREATED");
            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(String.valueOf(order.getId()));
            outboxEvent.setExchangeName(EVENTS_EXCHANGE);
            outboxEvent.setRoutingKey(ORDER_CREATED_ROUTING_KEY);
            outboxEvent.setPayload(objectMapper.writeValueAsString(envelope));
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot serialize order event", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
