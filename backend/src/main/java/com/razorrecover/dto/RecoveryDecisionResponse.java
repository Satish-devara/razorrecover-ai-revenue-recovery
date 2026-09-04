package com.razorrecover.dto;

import com.razorrecover.domain.enums.RecoveryAction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Reserved for Phase 4 onward; Phase 3 returns an empty history. */
public record RecoveryDecisionResponse(UUID id, RecoveryAction recommendedAction,
                                       RecoveryAction finalAction, BigDecimal confidence,
                                       String explanation, String safetyCheckSummary,
                                       String outcome, Instant createdAt) {
}
