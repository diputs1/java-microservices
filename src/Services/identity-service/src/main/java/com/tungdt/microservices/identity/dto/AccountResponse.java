package com.tungdt.microservices.identity.dto;

public record AccountResponse(Long id, String username, String email, String role) {
}
