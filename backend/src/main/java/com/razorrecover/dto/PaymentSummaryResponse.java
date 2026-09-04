package com.razorrecover.dto;

import com.razorrecover.domain.enums.PaymentFailureReason;
import com.razorrecover.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

/** Read model boundary; controllers will not expose JPA entities. */
public record PaymentSummaryResponse(
        UUID id,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentFailureReason failureReason) {
}
