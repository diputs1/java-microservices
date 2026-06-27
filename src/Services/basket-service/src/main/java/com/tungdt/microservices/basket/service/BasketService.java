package com.tungdt.microservices.basket.service;

import com.tungdt.microservices.basket.dto.BasketItemResponse;
import com.tungdt.microservices.basket.dto.BasketRequest;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.basket.entity.BasketEntity;
import com.tungdt.microservices.basket.entity.BasketItemEntity;
import com.tungdt.microservices.basket.repository.BasketRepository;
import com.tungdt.microservices.common.error.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BasketService {
    private static final Logger log = LoggerFactory.getLogger(BasketService.class);
    private final BasketRepository basketRepository;

    public BasketService(BasketRepository basketRepository) {
        this.basketRepository = basketRepository;
    }

    public BasketResponse save(BasketRequest request) {
        log.info("Save basket customerId={}", request.customerId());
        BasketEntity basket = toEntity(request);
        return toResponse(basketRepository.save(basket));
    }

    public BasketResponse getByCustomerId(String customerId) {
        return basketRepository.findByCustomerId(customerId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Basket not found", HttpStatus.NOT_FOUND));
    }

    public void delete(String customerId) {
        log.info("Delete basket customerId={}", customerId);
        basketRepository.delete(customerId);
    }

    private BasketEntity toEntity(BasketRequest request) {
        List<BasketItemEntity> items = request.items().stream()
                .map(item -> {
                    BasketItemEntity entity = new BasketItemEntity();
                    entity.setSku(item.sku());
                    entity.setProductName(item.productName());
                    entity.setQuantity(item.quantity());
                    entity.setUnitPrice(item.unitPrice());
                    return entity;
                })
                .toList();

        BasketEntity basket = new BasketEntity();
        basket.setCustomerId(request.customerId());
        basket.setItems(items);
        basket.setTotalAmount(calculateTotal(items));
        return basket;
    }

    private BasketResponse toResponse(BasketEntity basket) {
        List<BasketItemResponse> items = basket.getItems().stream()
                .map(item -> new BasketItemResponse(
                        item.getSku(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .toList();
        return new BasketResponse(basket.getCustomerId(), items, basket.getTotalAmount());
    }

    private BigDecimal calculateTotal(List<BasketItemEntity> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
