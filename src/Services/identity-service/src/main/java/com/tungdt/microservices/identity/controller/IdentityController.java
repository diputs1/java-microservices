package com.tungdt.microservices.identity.controller;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.identity.dto.AccountResponse;
import com.tungdt.microservices.identity.dto.LoginRequest;
import com.tungdt.microservices.identity.dto.LoginResponse;
import com.tungdt.microservices.identity.dto.RegisterRequest;
import com.tungdt.microservices.identity.service.IdentityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityController {
    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(identityService.register(request));
    }

    @PostMapping("/login")
    @Deprecated(since = "v1", forRemoval = false)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(identityService.login(request));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AccountResponse>> getAll() {
        return ApiResponse.ok(identityService.getAll());
    }
}
