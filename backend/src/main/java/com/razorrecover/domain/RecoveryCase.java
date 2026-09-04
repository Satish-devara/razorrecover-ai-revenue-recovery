package com.razorrecover.domain;

import com.razorrecover.domain.enums.RecoveryCaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_cases")
public class RecoveryCase extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryCaseStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void initializeOpenedAt() {
        if (openedAt == null) {
            openedAt = Instant.now();
        }
    }

    protected RecoveryCase() {
    }

    public static RecoveryCase open(Payment payment, Merchant merchant) {
        RecoveryCase recoveryCase = new RecoveryCase();
        recoveryCase.payment = payment;
        recoveryCase.merchant = merchant;
        recoveryCase.correlationId = payment.getId().toString();
        recoveryCase.status = RecoveryCaseStatus.OPEN;
        recoveryCase.openedAt = Instant.now();
        return recoveryCase;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public RecoveryCaseStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
