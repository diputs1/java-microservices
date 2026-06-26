package com.tungdt.microservices.email.controller;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.email.dto.EmailRequest;
import com.tungdt.microservices.email.dto.EmailResponse;
import com.tungdt.microservices.email.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal') or hasAuthority('SCOPE_service')")
    public ApiResponse<EmailResponse> send(@Valid @RequestBody EmailRequest request) {
        return ApiResponse.ok(emailService.send(request));
    }
}
