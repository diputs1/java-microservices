package com.tungdt.microservices.ordering.client;

import java.util.List;

public record InventoryReservationRequest(List<InventoryReservationItemRequest> items) {
}
