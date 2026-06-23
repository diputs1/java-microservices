package com.tungdt.microservices.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InventoryReservationItemRequest(
        @NotBlank String sku,
        @Min(1) Integer quantity
) {
}
