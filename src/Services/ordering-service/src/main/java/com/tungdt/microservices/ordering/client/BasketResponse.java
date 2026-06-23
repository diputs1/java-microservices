package com.tungdt.microservices.ordering.client;

import java.math.BigDecimal;
import java.util.List;

public record BasketResponse(String customerId, List<BasketItemResponse> items, BigDecimal totalAmount) {
}
