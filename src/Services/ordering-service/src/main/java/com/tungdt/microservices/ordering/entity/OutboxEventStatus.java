package com.tungdt.microservices.ordering.entity;

public enum OutboxEventStatus {
    PENDING,
    IN_PROGRESS,
    PUBLISHED,
    FAILED
}
