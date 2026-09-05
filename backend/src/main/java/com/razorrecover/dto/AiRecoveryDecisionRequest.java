package com.razorrecover.dto;

import com.razorrecover.domain.enums.RecoveryAction;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiRecoveryDecisionRequest(

        @NotNull
        RecoveryAction recommendedAction,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        double confidence,

        @NotBlank
        String reason,

        @NotBlank
        String policyId
) {
}
