package com.tungdt.microservices.customer.dto;

public record CustomerResponse(Long id, String email, String fullName, String phone) {
}
