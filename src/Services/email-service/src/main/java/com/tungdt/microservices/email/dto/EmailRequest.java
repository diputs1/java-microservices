package com.tungdt.microservices.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank @Email @Size(max = 150) String to,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 4000) String body
) {
}
