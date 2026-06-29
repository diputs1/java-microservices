package com.tungdt.microservices.ordering.saga;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.client.BasketClient;
import com.tungdt.microservices.ordering.client.BasketItemResponse;
import com.tungdt.microservices.ordering.client.BasketResponse;
import com.tungdt.microservices.ordering.client.InventoryClient;
import com.tungdt.microservices.ordering.client.InventoryReservationItemRequest;
import com.tungdt.microservices.ordering.dto.OrderRequest;
import com.tungdt.microservices.ordering.dto.OrderResponse;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.entity.OrderItemEntity;
import com.tungdt.microservices.ordering.entity.OrderStatus;
import com.tungdt.microservices.ordering.outbox.OrderOutboxService;
import com.tungdt.microservices.ordering.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderSagaService {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);

    private final OrderRepository orderRepository;
    private final BasketClient basketClient;
    private final InventoryClient inventoryClient;
    private final OrderOutboxService orderOutboxService;

    public OrderSagaService(OrderRepository orderRepository,
            BasketClient basketClient,
            InventoryClient inventoryClient,
            OrderOutboxService orderOutboxService) {
        this.orderRepository = orderRepository;
        this.basketClient = basketClient;
        this.inventoryClient = inventoryClient;
        this.orderOutboxService = orderOutboxService;
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public OrderResponse createOrder(OrderRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            OrderResponse existing = findExistingOrder(request, normalizedIdempotencyKey);
            if (existing != null) {
                return existing;
            }
        }

        String customerId = String.valueOf(request.customerId());
        BasketResponse basket = basketClient.getBasket(customerId);
        validateBasket(request, basket);

        List<InventoryReservationItemRequest> reservationItems = toReservationItems(basket);
        OrderEntity savedOrder = saveOrder(request, basket, normalizedIdempotencyKey);
        boolean inventoryReserved = false;

        try {
            inventoryClient.reserve(reservationItems);
            inventoryReserved = true;
            updateStatus(savedOrder, OrderStatus.INVENTORY_RESERVED);

            basketClient.deleteBasket(customerId);
            updateStatus(savedOrder, OrderStatus.COMPLETED);
            orderOutboxService.saveOrderCreated(savedOrder);
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

    private OrderResponse findExistingOrder(OrderRequest request, String idempotencyKey) {
        return orderRepository.findByCustomerIdAndIdempotencyKey(request.customerId(), idempotencyKey)
                .map(order -> {
                    if (request.totalAmount().compareTo(order.getTotalAmount()) != 0) {
                        throw new BusinessException("Idempotency key conflicts with existing order",
                                HttpStatus.CONFLICT);
                    }
                    log.info("Return existing order for idempotencyKey customerId={}", request.customerId());
                    return toResponse(order);
                })
                .orElse(null);
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

    private OrderEntity saveOrder(OrderRequest request, BasketResponse basket, String idempotencyKey) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.customerId());
        order.setTotalAmount(basket.totalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        order.setCreatedAt(Instant.now());
        basket.items().forEach(item -> order.addItem(toOrderItem(item)));
        return orderRepository.save(order);
    }

    private OrderItemEntity toOrderItem(BasketItemResponse item) {
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setSku(item.sku());
        orderItem.setProductName(item.productName());
        orderItem.setQuantity(item.quantity());
        orderItem.setUnitPrice(item.unitPrice());
        orderItem.setLineAmount(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        return orderItem;
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
        updateStatus(order, OrderStatus.FAILED);
    }

    private void updateStatus(OrderEntity order, OrderStatus status) {
        order.setStatus(status);
        orderRepository.save(order);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 128) {
            throw new BusinessException("Idempotency-Key must be at most 128 characters", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
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
