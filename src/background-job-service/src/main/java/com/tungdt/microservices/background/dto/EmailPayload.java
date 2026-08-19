package com.tungdt.microservices.background.dto;

public record EmailPayload(String to, String subject, String body) {
}
