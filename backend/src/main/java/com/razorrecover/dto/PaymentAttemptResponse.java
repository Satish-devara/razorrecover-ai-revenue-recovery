package com.razorrecover.dto;

import com.razorrecover.domain.enums.AttemptTriggerType;
import com.razorrecover.domain.enums.PaymentAttemptStatus;
import com.razorrecover.domain.enums.PaymentFailureReason;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(UUID id, int attemptNumber, AttemptTriggerType triggerType,
                                     PaymentAttemptStatus status, PaymentFailureReason failureReason,
                                     Instant attemptedAt) {
}
