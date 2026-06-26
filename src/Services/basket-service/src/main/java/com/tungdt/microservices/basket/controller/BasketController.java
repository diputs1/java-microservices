package com.tungdt.microservices.basket.controller;

import com.tungdt.microservices.basket.dto.BasketRequest;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.basket.service.BasketService;
import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.common.security.ResourceAccessGuard;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ResourceAccessGuard resourceAccessGuard;

    public BasketController(BasketService basketService, ResourceAccessGuard resourceAccessGuard) {
        this.basketService = basketService;
        this.resourceAccessGuard = resourceAccessGuard;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<BasketResponse> save(@Valid @RequestBody BasketRequest request) {
        assertCanAccessCustomer(request.customerId());
        return ApiResponse.ok(basketService.save(request));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<BasketResponse> getByCustomerId(@PathVariable String customerId) {
        assertCanAccessCustomer(customerId);
        return ApiResponse.ok(basketService.getByCustomerId(customerId));
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<Void> delete(@PathVariable String customerId) {
        assertCanAccessCustomer(customerId);
        basketService.delete(customerId);
        return ApiResponse.ok(null);
    }

    private void assertCanAccessCustomer(String customerId) {
        if (!resourceAccessGuard.canAccessOwner(customerId)) {
            throw new BusinessException("Access denied for customer resource", HttpStatus.FORBIDDEN);
        }
    }
}
