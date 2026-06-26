package com.tungdt.microservices.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductCatalogResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Instant updatedAt
) {
}
