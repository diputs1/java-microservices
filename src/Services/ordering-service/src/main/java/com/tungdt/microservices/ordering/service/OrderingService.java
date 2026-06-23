package com.tungdt.microservices.ordering.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.dto.OrderRequest;
import com.tungdt.microservices.ordering.dto.OrderResponse;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.tungdt.microservices.ordering.saga.OrderSagaService;

@Service
public class OrderingService {
    private static final Logger log = LoggerFactory.getLogger(OrderingService.class);
    private final OrderRepository orderRepository;
    private final OrderSagaService orderSagaService;

    public OrderingService(OrderRepository orderRepository, OrderSagaService orderSagaService) {
        this.orderRepository = orderRepository;
        this.orderSagaService = orderSagaService;
    }

    public OrderResponse create(OrderRequest request) {
        log.info("Create order customerId={}", request.customerId());
        return orderSagaService.createOrder(request);
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OrderResponse getById(Long id) {
        return toResponse(findById(id));
    }

    private OrderEntity findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
    }

    private OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
