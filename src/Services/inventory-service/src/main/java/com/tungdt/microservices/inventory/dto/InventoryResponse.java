package com.tungdt.microservices.inventory.dto;

public record InventoryResponse(String id, String sku, Integer availableQuantity, String location) {
}
