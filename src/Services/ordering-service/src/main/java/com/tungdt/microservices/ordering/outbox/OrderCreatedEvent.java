package com.tungdt.microservices.ordering.outbox;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(Long orderId, Long customerId, BigDecimal totalAmount, Instant createdAt) {
}
