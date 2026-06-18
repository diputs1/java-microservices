package com.tungdt.microservices.basket.dto;

import java.math.BigDecimal;

public record BasketItemResponse(String sku, String productName, Integer quantity, BigDecimal unitPrice) {
}
