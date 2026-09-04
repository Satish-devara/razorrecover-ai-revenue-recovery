package com.razorrecover.dto;

import com.razorrecover.domain.enums.PaymentFailureReason;
import com.razorrecover.domain.enums.PaymentStatus;
import com.razorrecover.domain.enums.SimulationScenario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, String providerPaymentId, BigDecimal amount, String currency,
                              PaymentStatus status, PaymentFailureReason failureReason,
                              SimulationScenario scenario, Instant failedAt, Instant createdAt,
                              Instant updatedAt) {
}
