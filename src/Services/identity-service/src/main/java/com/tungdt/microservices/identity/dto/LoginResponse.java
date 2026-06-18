package com.tungdt.microservices.identity.dto;

public record LoginResponse(String accessToken, String tokenType, AccountResponse account) {
}
