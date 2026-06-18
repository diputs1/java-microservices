package com.tungdt.microservices.background.dto;

import java.time.Instant;

public record JobEventResponse(String id, String type, String payload, Instant receivedAt) {
}
