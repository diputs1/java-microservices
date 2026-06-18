package com.tungdt.microservices.ordering.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.dto.OrderRequest;
import com.tungdt.microservices.ordering.dto.OrderResponse;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderingService {
    private static final Logger log = LoggerFactory.getLogger(OrderingService.class);
    private static final String ORDER_CREATED_QUEUE = "order.created";
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderingService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        log.info("Create order customerId={}", request.customerId());
        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.customerId());
        order.setTotalAmount(request.totalAmount());
        order.setStatus("CREATED");
        order.setCreatedAt(Instant.now());
        OrderEntity saved = orderRepository.save(order);
        rabbitTemplate.convertAndSend(ORDER_CREATED_QUEUE, "order:" + saved.getId());
        return toResponse(saved);
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
