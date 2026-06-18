package com.tungdt.microservices.inventory.repository;

import com.tungdt.microservices.inventory.entity.InventoryEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryEntity, String> {
    Optional<InventoryEntity> findBySku(String sku);

    boolean existsBySku(String sku);
}
