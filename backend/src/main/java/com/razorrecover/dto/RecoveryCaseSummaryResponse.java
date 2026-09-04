package com.razorrecover.dto;

import com.razorrecover.domain.enums.RecoveryAction;
import com.razorrecover.domain.enums.RecoveryCaseStatus;
import java.util.UUID;

/** Read model boundary for the future recovery-case API. */
public record RecoveryCaseSummaryResponse(
        UUID id,
        UUID paymentId,
        String correlationId,
        RecoveryCaseStatus status,
        int retryCount,
        RecoveryAction finalAction) {
}
