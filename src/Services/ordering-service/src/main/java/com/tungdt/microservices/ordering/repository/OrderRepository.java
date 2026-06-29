package com.tungdt.microservices.ordering.repository;

import com.tungdt.microservices.ordering.entity.OrderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);
}
