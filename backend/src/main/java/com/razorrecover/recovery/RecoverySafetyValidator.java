package com.razorrecover.recovery;

import com.razorrecover.domain.Payment;
import com.razorrecover.domain.RecoveryCase;
import com.razorrecover.domain.enums.PaymentStatus;
import com.razorrecover.domain.enums.RecoveryAction;

import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class RecoverySafetyValidator {

    private final RecoveryPolicyConfig policyConfig;

    public RecoverySafetyValidator(RecoveryPolicyConfig policyConfig) {
        this.policyConfig = policyConfig;
    }

    public ValidationResult validate(
            RecoveryCase recoveryCase,
            Payment payment,
            RecoveryAction action,
            Instant now) {

        if (recoveryCase.getStatus().isTerminal()) {
            return ValidationResult.rejected(
                    "CASE_TERMINAL",
                    "Recovery case is already in a terminal state.");
        }

        if (payment.getStatus() != PaymentStatus.FAILED) {
            return ValidationResult.rejected(
                    "PAYMENT_NOT_FAILED",
                    "Only failed payments are eligible for recovery.");
        }

        if (action != RecoveryAction.RETRY_PAYMENT) {
            return ValidationResult.allowed(
                    "ACTION_DOES_NOT_EXECUTE_PAYMENT",
                    "The selected action does not require an automatic payment retry.");
        }

        if (recoveryCase.getRetryCount() >= policyConfig.getMaxRetries()) {
            return ValidationResult.rejected(
                    "MAX_RETRIES_EXCEEDED",
                    "Maximum recovery retry limit has been reached.");
        }

        Instant recoveryExpiresAt = payment.getRecoveryExpiresAt();

        if (recoveryExpiresAt != null && now.isAfter(recoveryExpiresAt)) {
            return ValidationResult.rejected(
                    "RECOVERY_WINDOW_EXPIRED",
                    "The payment is outside its recovery window.");
        }

        if (payment.getFailedAt() != null) {
            Instant cooldownEndsAt =
                    payment.getFailedAt().plus(policyConfig.getCooldown());

            if (now.isBefore(cooldownEndsAt)) {
                return ValidationResult.rejected(
                        "COOLDOWN_ACTIVE",
                        "Recovery cooldown is still active.");
            }
        }

        return ValidationResult.allowed(
                "SAFETY_CHECK_PASSED",
                "Payment retry satisfies the recovery safety policy.");
    }

    public record ValidationResult(
            boolean allowed,
            String code,
            String message) {

        public static ValidationResult allowed(String code, String message) {
            return new ValidationResult(true, code, message);
        }

        public static ValidationResult rejected(String code, String message) {
            return new ValidationResult(false, code, message);
        }
    }
}
