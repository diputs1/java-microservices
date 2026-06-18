package com.tungdt.microservices.ordering.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(Long id, Long customerId, BigDecimal totalAmount, String status, Instant createdAt) {
}
