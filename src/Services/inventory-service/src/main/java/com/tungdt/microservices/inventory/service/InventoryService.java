package com.tungdt.microservices.inventory.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.inventory.dto.InventoryRequest;
import com.tungdt.microservices.inventory.dto.InventoryResponse;
import com.tungdt.microservices.inventory.entity.InventoryEntity;
import com.tungdt.microservices.inventory.repository.InventoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse create(InventoryRequest request) {
        if (inventoryRepository.existsBySku(request.sku())) {
            throw new BusinessException("Inventory sku already exists", HttpStatus.CONFLICT);
        }
        log.info("Create inventory sku={}", request.sku());
        InventoryEntity item = new InventoryEntity();
        apply(request, item);
        return toResponse(inventoryRepository.save(item));
    }

    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InventoryResponse getBySku(String sku) {
        return toResponse(inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new BusinessException("Inventory not found", HttpStatus.NOT_FOUND)));
    }

    public InventoryResponse update(String id, InventoryRequest request) {
        InventoryEntity item = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Inventory not found", HttpStatus.NOT_FOUND));
        log.info("Update inventory id={}", id);
        apply(request, item);
        return toResponse(inventoryRepository.save(item));
    }

    private void apply(InventoryRequest request, InventoryEntity item) {
        item.setSku(request.sku());
        item.setAvailableQuantity(request.availableQuantity());
        item.setLocation(request.location());
    }

    private InventoryResponse toResponse(InventoryEntity item) {
        return new InventoryResponse(item.getId(), item.getSku(), item.getAvailableQuantity(), item.getLocation());
    }
}
