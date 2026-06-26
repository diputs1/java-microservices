package com.tungdt.microservices.inventory.controller;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.inventory.dto.InventoryRequest;
import com.tungdt.microservices.inventory.dto.InventoryReservationRequest;
import com.tungdt.microservices.inventory.dto.InventoryReservationResponse;
import com.tungdt.microservices.inventory.dto.InventoryResponse;
import com.tungdt.microservices.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.created(inventoryService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<List<InventoryResponse>> getAll() {
        return ApiResponse.ok(inventoryService.getAll());
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<InventoryResponse> getBySku(@PathVariable String sku) {
        return ApiResponse.ok(inventoryService.getBySku(sku));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> update(@PathVariable String id, @Valid @RequestBody InventoryRequest request) {
        return ApiResponse.ok(inventoryService.update(id, request));
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<InventoryReservationResponse> reserve(@Valid @RequestBody InventoryReservationRequest request) {
        return ApiResponse.ok(inventoryService.reserve(request));
    }

    @PostMapping("/reservations/release")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<InventoryReservationResponse> release(@Valid @RequestBody InventoryReservationRequest request) {
        return ApiResponse.ok(inventoryService.release(request));
    }
}
