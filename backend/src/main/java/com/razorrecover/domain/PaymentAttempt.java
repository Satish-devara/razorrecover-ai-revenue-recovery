package com.razorrecover.domain;

import com.razorrecover.domain.enums.AttemptTriggerType;
import com.razorrecover.domain.enums.PaymentAttemptStatus;
import com.razorrecover.domain.enums.PaymentFailureReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import com.razorrecover.payment.provider.PaymentExecutionResult;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 40)
    private AttemptTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentAttemptStatus status;

    @Column(name = "provider_attempt_id", length = 120)
    private String providerAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 80)
    private PaymentFailureReason failureReason;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @PrePersist
    void initializeAttemptedAt() {
        if (attemptedAt == null) {
            attemptedAt = Instant.now();
        }
    }

    protected PaymentAttempt() {
    }

    public static PaymentAttempt create(
            Payment payment,
            int attemptNumber,
            AttemptTriggerType triggerType,
            PaymentExecutionResult result) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.payment = payment;
        attempt.attemptNumber = attemptNumber;
        attempt.triggerType = triggerType;
        attempt.status = result.successful() ? PaymentAttemptStatus.SUCCEEDED : PaymentAttemptStatus.FAILED;
        attempt.failureReason = result.failureReason();
        attempt.attemptedAt = Instant.now();
        return attempt;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptTriggerType getTriggerType() {
        return triggerType;
    }

    public PaymentAttemptStatus getStatus() {
        return status;
    }

    public PaymentFailureReason getFailureReason() {
        return failureReason;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
