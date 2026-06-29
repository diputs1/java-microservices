package com.tungdt.microservices.ordering.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventEnvelope(
        UUID eventId,
        String eventType,
        int version,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String traceId,
        Object data
) {
}
