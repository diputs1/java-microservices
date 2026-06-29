package com.tungdt.microservices.background.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        int version,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        JsonNode data
) {
}
