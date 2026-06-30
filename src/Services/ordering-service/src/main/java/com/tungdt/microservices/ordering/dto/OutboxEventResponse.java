package com.tungdt.microservices.ordering.dto;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String exchangeName,
        String routingKey,
        String status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant publishedAt
) {
    public static OutboxEventResponse from(OutboxEventEntity event) {
        return new OutboxEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getExchangeName(),
                event.getRoutingKey(),
                event.getStatus(),
                event.getAttempts(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getPublishedAt()
        );
    }
}
