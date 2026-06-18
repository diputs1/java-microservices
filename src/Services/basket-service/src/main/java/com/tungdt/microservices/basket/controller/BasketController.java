package com.tungdt.microservices.basket.controller;

import com.tungdt.microservices.basket.dto.BasketRequest;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.basket.service.BasketService;
import com.tungdt.microservices.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/baskets")
public class BasketController {
    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @PostMapping
    public ApiResponse<BasketResponse> save(@Valid @RequestBody BasketRequest request) {
        return ApiResponse.ok(basketService.save(request));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<BasketResponse> getByCustomerId(@PathVariable String customerId) {
        return ApiResponse.ok(basketService.getByCustomerId(customerId));
    }

    @DeleteMapping("/{customerId}")
    public ApiResponse<Void> delete(@PathVariable String customerId) {
        basketService.delete(customerId);
        return ApiResponse.ok(null);
    }
}
