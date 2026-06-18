package com.tungdt.microservices.basket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BasketItemRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 150) String productName,
        @NotNull @Min(1) Integer quantity,
        @NotNull BigDecimal unitPrice
) {
}
