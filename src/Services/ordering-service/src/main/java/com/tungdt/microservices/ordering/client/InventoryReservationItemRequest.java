package com.tungdt.microservices.ordering.client;

public record InventoryReservationItemRequest(String sku, Integer quantity) {
}
