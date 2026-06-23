package com.tungdt.microservices.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InventoryReservationRequest(
        @NotEmpty List<@Valid InventoryReservationItemRequest> items
) {
}
