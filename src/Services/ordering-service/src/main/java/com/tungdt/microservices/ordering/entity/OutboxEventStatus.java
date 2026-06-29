package com.tungdt.microservices.ordering.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
