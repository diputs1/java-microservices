package com.tungdt.microservices.ordering.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderRequest(
        @NotNull Long customerId,
        @NotNull @DecimalMin("0.0") BigDecimal totalAmount
) {
}
