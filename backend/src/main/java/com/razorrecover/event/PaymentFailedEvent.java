package com.razorrecover.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String failureReason,
        String scenario,
        Instant failedAt,
        String correlationId
) {
}