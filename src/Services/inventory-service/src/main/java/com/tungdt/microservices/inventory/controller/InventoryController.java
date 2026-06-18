package com.tungdt.microservices.inventory.controller;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.inventory.dto.InventoryRequest;
import com.tungdt.microservices.inventory.dto.InventoryResponse;
import com.tungdt.microservices.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ApiResponse<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.created(inventoryService.create(request));
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> getAll() {
        return ApiResponse.ok(inventoryService.getAll());
    }

    @GetMapping("/sku/{sku}")
    public ApiResponse<InventoryResponse> getBySku(@PathVariable String sku) {
        return ApiResponse.ok(inventoryService.getBySku(sku));
    }

    @PutMapping("/{id}")
    public ApiResponse<InventoryResponse> update(@PathVariable String id, @Valid @RequestBody InventoryRequest request) {
        return ApiResponse.ok(inventoryService.update(id, request));
    }
}
