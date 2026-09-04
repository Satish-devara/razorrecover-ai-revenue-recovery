package com.razorrecover.domain;

import com.razorrecover.domain.enums.RecoveryAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "recovery_decisions")
public class RecoveryDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_policy_id")
    private RecoveryPolicy recoveryPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 40)
    private RecoveryAction recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_action", length = 40)
    private RecoveryAction finalAction;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(name = "safety_check_summary", columnDefinition = "text")
    private String safetyCheckSummary;

    @Column(length = 40)
    private String outcome;

    protected RecoveryDecision() {
    }

    public static RecoveryDecision create(
            RecoveryCase recoveryCase,
            RecoveryAction recommendedAction,
            RecoveryAction finalAction,
            BigDecimal confidence,
            String explanation,
            String safetyCheckSummary,
            String outcome) {

        RecoveryDecision decision = new RecoveryDecision();

        decision.recoveryCase = recoveryCase;
        decision.recommendedAction = recommendedAction;
        decision.finalAction = finalAction;
        decision.confidence = confidence;
        decision.explanation = explanation;
        decision.safetyCheckSummary = safetyCheckSummary;
        decision.outcome = outcome;

        return decision;
    }

    public RecoveryAction getRecommendedAction() {
        return recommendedAction;
    }

    public RecoveryAction getFinalAction() {
        return finalAction;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSafetyCheckSummary() {
        return safetyCheckSummary;
    }

    public String getOutcome() {
        return outcome;
    }
}
