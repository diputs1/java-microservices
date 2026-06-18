package com.tungdt.microservices.basket.service;

import com.tungdt.microservices.basket.dto.BasketItemResponse;
import com.tungdt.microservices.basket.dto.BasketRequest;
import com.tungdt.microservices.basket.dto.BasketResponse;
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
        List<BasketItemResponse> items = request.items().stream()
                .map(item -> new BasketItemResponse(item.sku(), item.productName(), item.quantity(), item.unitPrice()))
                .toList();
        BasketResponse basket = new BasketResponse(request.customerId(), items, calculateTotal(items));
        return basketRepository.save(basket);
    }

    public BasketResponse getByCustomerId(String customerId) {
        return basketRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("Basket not found", HttpStatus.NOT_FOUND));
    }

    public void delete(String customerId) {
        log.info("Delete basket customerId={}", customerId);
        basketRepository.delete(customerId);
    }

    private BigDecimal calculateTotal(List<BasketItemResponse> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
