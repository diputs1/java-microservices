package com.tungdt.microservices.inventory.dto;

import java.util.List;

public record InventoryReservationResponse(String status, List<InventoryReservationItemRequest> items) {
}
