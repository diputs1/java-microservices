package com.tungdt.microservices.ordering.controller;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.ordering.dto.OutboxEventResponse;
import com.tungdt.microservices.ordering.dto.OutboxRetryResponse;
import com.tungdt.microservices.ordering.service.OutboxOperationsService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/outbox")
public class OutboxOperationsController {
    private final OutboxOperationsService outboxOperationsService;

    public OutboxOperationsController(OutboxOperationsService outboxOperationsService) {
        this.outboxOperationsService = outboxOperationsService;
    }

    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OutboxEventResponse>> getFailedEvents() {
        return ApiResponse.ok(outboxOperationsService.getFailedEvents());
    }

    @PostMapping("/failed/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OutboxRetryResponse> retryFailedEvents() {
        return ApiResponse.ok(outboxOperationsService.retryFailedEvents());
    }
}
