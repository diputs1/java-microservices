package com.tungdt.microservices.ordering.client;

import java.math.BigDecimal;

public record BasketItemResponse(String sku, String productName, Integer quantity, BigDecimal unitPrice) {
}
