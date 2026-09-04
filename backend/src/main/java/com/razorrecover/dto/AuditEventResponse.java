package com.razorrecover.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, String aggregateType, UUID aggregateId,
                                 String eventType, String actor, String payload, Instant occurredAt) {
}
