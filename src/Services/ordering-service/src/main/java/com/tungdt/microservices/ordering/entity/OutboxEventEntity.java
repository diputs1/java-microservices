package com.tungdt.microservices.ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID eventId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, length = 128)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @PrePersist
    void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (status == null) {
            status = OutboxEventStatus.PENDING.name();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status.name();
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
