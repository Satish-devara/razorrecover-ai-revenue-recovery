package com.razorrecover.recovery;

import com.razorrecover.audit.AuditService;
import com.razorrecover.domain.Payment;
import com.razorrecover.domain.RecoveryCase;
import com.razorrecover.domain.RecoveryDecision;
import com.razorrecover.domain.enums.AuditEventType;
import com.razorrecover.domain.enums.PaymentStatus;
import com.razorrecover.domain.enums.RecoveryAction;
import com.razorrecover.dto.RecoveryDecisionResponse;
import com.razorrecover.idempotency.IdempotencyService;
import com.razorrecover.payment.PaymentService;
import com.razorrecover.repository.RecoveryCaseRepository;
import com.razorrecover.repository.RecoveryDecisionRepository;
import com.razorrecover.support.InvalidStateException;
import com.razorrecover.support.NotFoundException;
import com.razorrecover.support.OperationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RecoveryEngine {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryDecisionRepository recoveryDecisionRepository;
    private final FailureClassifier failureClassifier;
    private final RecoverySafetyValidator safetyValidator;
    private final PaymentService paymentService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    public RecoveryEngine(
            RecoveryCaseRepository recoveryCaseRepository,
            RecoveryDecisionRepository recoveryDecisionRepository,
            FailureClassifier failureClassifier,
            RecoverySafetyValidator safetyValidator,
            PaymentService paymentService,
            AuditService auditService,
            IdempotencyService idempotencyService) {

        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryDecisionRepository = recoveryDecisionRepository;
        this.failureClassifier = failureClassifier;
        this.safetyValidator = safetyValidator;
        this.paymentService = paymentService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public RecoveryDecisionResponse evaluate(
            UUID recoveryCaseId,
            OperationContext context) {

        /*
         * ---------------------------------------------------------
         * 1. IDEMPOTENCY CHECK
         * ---------------------------------------------------------
         *
         * If this request already used the same Idempotency-Key,
         * do not execute another recovery operation.
         */
        String idempotencyKey = context.idempotencyKey();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            var existingResult =
                    idempotencyService.get(idempotencyKey);

            if (existingResult.isPresent()) {

                throw new InvalidStateException(
                        "Duplicate recovery operation: idempotency key already used"
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * 2. LOAD RECOVERY CASE
         * ---------------------------------------------------------
         */

        RecoveryCase recoveryCase =
                recoveryCaseRepository.findById(recoveryCaseId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Recovery case",
                                        recoveryCaseId
                                ));

        Payment payment = recoveryCase.getPayment();

        /*
         * ---------------------------------------------------------
         * 3. SAFETY CHECK — TERMINAL CASE
         * ---------------------------------------------------------
         */

        if (recoveryCase.getStatus().isTerminal()) {

            throw new InvalidStateException(
                    "Recovery case is already terminal"
            );
        }

        /*
         * ---------------------------------------------------------
         * 4. SAFETY CHECK — PAYMENT MUST BE FAILED
         * ---------------------------------------------------------
         */

        if (payment.getStatus() != PaymentStatus.FAILED) {

            throw new InvalidStateException(
                    "Recovery can only be evaluated for a failed payment"
            );
        }

        /*
         * ---------------------------------------------------------
         * 5. CLASSIFY FAILURE
         * ---------------------------------------------------------
         */

        RecoveryAction recommendedAction =
                failureClassifier.classify(
                        payment.getFailureReason()
                );

        /*
         * ---------------------------------------------------------
         * 6. RUN DETERMINISTIC SAFETY VALIDATION
         * ---------------------------------------------------------
         */

        RecoverySafetyValidator.ValidationResult safetyResult =
                safetyValidator.validate(
                        recoveryCase,
                        payment,
                        recommendedAction,
                        Instant.now()
                );

        RecoveryAction finalAction = recommendedAction;
        String outcome;

        /*
         * ---------------------------------------------------------
         * 7. SAFETY BLOCK
         * ---------------------------------------------------------
         */

        if (!safetyResult.allowed()) {

            finalAction =
                    determineBlockedAction(
                            recommendedAction,
                            safetyResult.code()
                    );

            outcome = "BLOCKED";

            auditService.record(
                    recoveryCase.getCorrelationId(),
                    "RECOVERY_CASE",
                    recoveryCase.getId(),
                    AuditEventType.RECOVERY_RETRY_BLOCKED,
                    Map.of(
                            "recommendedAction",
                            recommendedAction.name(),

                            "finalAction",
                            finalAction.name(),

                            "reasonCode",
                            safetyResult.code()
                    )
            );
        }

        /*
         * ---------------------------------------------------------
         * 8. AUTOMATIC PAYMENT RETRY
         * ---------------------------------------------------------
         */

        else if (recommendedAction ==
                RecoveryAction.RETRY_PAYMENT) {

            recoveryCase.markRetryPending();

            recoveryCaseRepository.save(recoveryCase);

            /*
             * Execute the actual payment retry.
             */
            paymentService.retry(
                    payment.getId(),
                    context
            );

            Payment refreshedPayment =
                    recoveryCase.getPayment();

            /*
             * -----------------------------------------------------
             * RETRY SUCCESS
             * -----------------------------------------------------
             */

            if (refreshedPayment.getStatus() ==
                    PaymentStatus.SUCCEEDED) {

                recoveryCase.markRecovered();

                outcome = "RECOVERED";
            }

            /*
             * -----------------------------------------------------
             * RETRY FAILED
             * -----------------------------------------------------
             */

            else {

                outcome = "RETRY_FAILED";
            }

            recoveryCaseRepository.save(recoveryCase);
        }

        /*
         * ---------------------------------------------------------
         * 9. CUSTOMER ACTION REQUIRED
         * ---------------------------------------------------------
         */

        else if (recommendedAction ==
                RecoveryAction.REQUEST_CUSTOMER_ACTION) {

            finalAction =
                    RecoveryAction.REQUEST_CUSTOMER_ACTION;

            recoveryCase.markEscalated();

            recoveryCaseRepository.save(recoveryCase);

            outcome = "CUSTOMER_ACTION_REQUIRED";
        }

        /*
         * ---------------------------------------------------------
         * 10. ESCALATION
         * ---------------------------------------------------------
         */

        else {

            finalAction = RecoveryAction.ESCALATE;

            recoveryCase.markEscalated();

            recoveryCaseRepository.save(recoveryCase);

            outcome = "ESCALATED";
        }

        /*
         * ---------------------------------------------------------
         * 11. CREATE RECOVERY DECISION
         * ---------------------------------------------------------
         */

        RecoveryDecision decision =
                createDecision(
                        recoveryCase,
                        recommendedAction,
                        finalAction,
                        safetyResult,
                        outcome
                );

        recoveryDecisionRepository.save(decision);

        /*
         * ---------------------------------------------------------
         * 12. CLAIM IDEMPOTENCY KEY
         * ---------------------------------------------------------
         *
         * Store the decision ID in Redis.
         *
         * SETNX semantics mean only the first request can claim
         * this key.
         */
        if (idempotencyKey != null
                && !idempotencyKey.isBlank()) {

            boolean claimed =
                    idempotencyService.claim(
                            idempotencyKey,
                            decision.getId().toString()
                    );

            if (!claimed) {

                throw new InvalidStateException(
                        "Duplicate recovery operation: idempotency key already used"
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * 13. AUDIT DECISION
         * ---------------------------------------------------------
         */

        auditService.record(
                recoveryCase.getCorrelationId(),
                "RECOVERY_CASE",
                recoveryCase.getId(),
                AuditEventType.RECOVERY_DECISION_CREATED,
                Map.of(
                        "recommendedAction",
                        recommendedAction.name(),

                        "finalAction",
                        finalAction.name(),

                        "outcome",
                        outcome,

                        "safetyCode",
                        safetyResult.code()
                )
        );

        /*
         * ---------------------------------------------------------
         * 14. RETURN RESULT
         * ---------------------------------------------------------
         */

        return new RecoveryDecisionResponse(
                decision.getId(),
                decision.getRecommendedAction(),
                decision.getFinalAction(),
                decision.getConfidence(),
                decision.getExplanation(),
                decision.getSafetyCheckSummary(),
                decision.getOutcome(),
                decision.getCreatedAt()
        );
    }

    private RecoveryAction determineBlockedAction(
            RecoveryAction recommendedAction,
            String safetyCode) {

        if (recommendedAction ==
                RecoveryAction.RETRY_PAYMENT) {

            return switch (safetyCode) {

                case "MAX_RETRIES_EXCEEDED",
                     "RECOVERY_WINDOW_EXPIRED",
                     "CASE_TERMINAL" ->
                        RecoveryAction.STOP;

                default ->
                        RecoveryAction.STOP;
            };
        }

        return recommendedAction;
    }

    private RecoveryDecision createDecision(
            RecoveryCase recoveryCase,
            RecoveryAction recommendedAction,
            RecoveryAction finalAction,
            RecoverySafetyValidator.ValidationResult safetyResult,
            String outcome) {

        return RecoveryDecision.create(
                recoveryCase,
                recommendedAction,
                finalAction,
                BigDecimal.ONE,
                "Deterministic recovery policy classified the payment failure and selected a bounded recovery action.",
                safetyResult.code()
                        + ": "
                        + safetyResult.message(),
                outcome
        );
    }
}