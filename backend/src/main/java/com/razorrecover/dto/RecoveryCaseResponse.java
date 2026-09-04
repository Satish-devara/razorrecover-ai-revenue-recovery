package com.razorrecover.dto;

import com.razorrecover.domain.enums.RecoveryCaseStatus;
import java.time.Instant;
import java.util.UUID;

public record RecoveryCaseResponse(UUID id, UUID paymentId, String correlationId,
                                   RecoveryCaseStatus status, int retryCount,
                                   Instant openedAt, Instant closedAt) {
}
