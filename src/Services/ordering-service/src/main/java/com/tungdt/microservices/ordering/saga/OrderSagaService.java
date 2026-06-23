package com.tungdt.microservices.ordering.saga;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.client.BasketClient;
import com.tungdt.microservices.ordering.client.BasketResponse;
import com.tungdt.microservices.ordering.client.InventoryClient;
import com.tungdt.microservices.ordering.client.InventoryReservationItemRequest;
import com.tungdt.microservices.ordering.dto.OrderRequest;
import com.tungdt.microservices.ordering.dto.OrderResponse;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaService {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);
    private static final String ORDER_CREATED_QUEUE = "order.created";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final OrderRepository orderRepository;
    private final BasketClient basketClient;
    private final InventoryClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderSagaService(OrderRepository orderRepository,
            BasketClient basketClient,
            InventoryClient inventoryClient,
            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.basketClient = basketClient;
        this.inventoryClient = inventoryClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public OrderResponse createOrder(OrderRequest request) {
        String customerId = String.valueOf(request.customerId());
        BasketResponse basket = basketClient.getBasket(customerId);
        validateBasket(request, basket);

        List<InventoryReservationItemRequest> reservationItems = toReservationItems(basket);
        OrderEntity savedOrder = null;
        boolean inventoryReserved = false;

        try {
            inventoryClient.reserve(reservationItems);
            inventoryReserved = true;

            savedOrder = saveOrder(request, basket);
            basketClient.deleteBasket(customerId);
            rabbitTemplate.convertAndSend(ORDER_CREATED_QUEUE, "order:" + savedOrder.getId());
            log.info("Order saga completed orderId={} customerId={}", savedOrder.getId(), customerId);
            return toResponse(savedOrder);
        } catch (RuntimeException ex) {
            if (inventoryReserved) {
                releaseInventory(reservationItems, savedOrder);
            }
            if (savedOrder != null) {
                markFailed(savedOrder);
            }
            throw ex;
        }
    }

    private void validateBasket(OrderRequest request, BasketResponse basket) {
        if (basket.items() == null || basket.items().isEmpty()) {
            throw new BusinessException("Basket is empty", HttpStatus.BAD_REQUEST);
        }
        if (request.totalAmount().compareTo(basket.totalAmount()) != 0) {
            throw new BusinessException("Order total amount does not match basket", HttpStatus.BAD_REQUEST);
        }
    }

    private List<InventoryReservationItemRequest> toReservationItems(BasketResponse basket) {
        Map<String, Integer> quantityBySku = basket.items().stream()
                .collect(Collectors.groupingBy(
                        item -> item.sku(),
                        Collectors.summingInt(item -> item.quantity())
                ));
        return quantityBySku.entrySet().stream()
                .map(entry -> new InventoryReservationItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    private OrderEntity saveOrder(OrderRequest request, BasketResponse basket) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.customerId());
        order.setTotalAmount(basket.totalAmount());
        order.setStatus(STATUS_COMPLETED);
        order.setCreatedAt(Instant.now());
        return orderRepository.save(order);
    }

    private void releaseInventory(List<InventoryReservationItemRequest> reservationItems, OrderEntity savedOrder) {
        try {
            inventoryClient.release(reservationItems);
        } catch (RuntimeException compensationFailure) {
            Long orderId = savedOrder == null ? null : savedOrder.getId();
            log.error("Order saga compensation failed orderId={}", orderId, compensationFailure);
        }
    }

    private void markFailed(OrderEntity order) {
        order.setStatus(STATUS_FAILED);
        orderRepository.save(order);
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
