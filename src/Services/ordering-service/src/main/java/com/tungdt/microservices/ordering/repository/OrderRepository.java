package com.tungdt.microservices.ordering.repository;

import com.tungdt.microservices.ordering.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
