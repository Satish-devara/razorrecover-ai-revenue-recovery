package com.razorrecover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void initializeOccurredAt() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    protected AuditEvent() {
    }

    public static AuditEvent create(
            String correlationId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String actor,
            String payload) {
        AuditEvent event = new AuditEvent();
        event.correlationId = correlationId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.actor = actor;
        event.payload = payload;
        event.occurredAt = Instant.now();
        return event;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
