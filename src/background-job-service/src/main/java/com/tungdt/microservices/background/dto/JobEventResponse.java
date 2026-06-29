package com.tungdt.microservices.background.dto;

import java.time.Instant;

public record JobEventResponse(String id, String eventId, String traceId, String type, String routingKey, String payload,
                               Instant receivedAt) {
}
