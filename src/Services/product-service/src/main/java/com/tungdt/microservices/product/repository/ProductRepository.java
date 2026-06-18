package com.tungdt.microservices.product.repository;

import com.tungdt.microservices.product.entity.ProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findBySku(String sku);

    boolean existsBySku(String sku);
}
