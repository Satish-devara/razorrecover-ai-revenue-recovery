package com.razorrecover.recovery;

import com.razorrecover.audit.AuditService;
import com.razorrecover.domain.AuditEvent;
import com.razorrecover.domain.Payment;
import com.razorrecover.domain.RecoveryCase;
import com.razorrecover.domain.RecoveryDecision;
import com.razorrecover.domain.enums.AuditEventType;
import com.razorrecover.domain.enums.PaymentStatus;
import com.razorrecover.dto.AuditEventResponse;
import com.razorrecover.dto.CreateRecoveryCaseRequest;
import com.razorrecover.dto.RecoveryCaseResponse;
import com.razorrecover.dto.RecoveryDecisionResponse;
import com.razorrecover.event.PaymentFailedEvent;
import com.razorrecover.repository.AuditEventRepository;
import com.razorrecover.repository.PaymentRepository;
import com.razorrecover.repository.RecoveryCaseRepository;
import com.razorrecover.repository.RecoveryDecisionRepository;
import com.razorrecover.support.InvalidStateException;
import com.razorrecover.support.NotFoundException;
import com.razorrecover.support.OperationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecoveryCaseService {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryDecisionRepository recoveryDecisionRepository;
    private final AuditEventRepository auditEventRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public RecoveryCaseService(
            RecoveryCaseRepository recoveryCaseRepository,
            RecoveryDecisionRepository recoveryDecisionRepository,
            AuditEventRepository auditEventRepository,
            PaymentRepository paymentRepository,
            AuditService auditService) {

        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryDecisionRepository = recoveryDecisionRepository;
        this.auditEventRepository = auditEventRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RecoveryCaseResponse open(
            CreateRecoveryCaseRequest request,
            OperationContext context) {

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Payment",
                                request.paymentId()
                        ));

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException(
                    "Recovery case can only be opened for a failed payment"
            );
        }

        /*
         * A payment can have only one recovery case.
         *
         * Kafka may already have created the recovery case from
         * payment.failed. Return the existing case instead of
         * attempting another INSERT.
         */
        var existingCase =
                recoveryCaseRepository.findByPaymentId(payment.getId());

        if (existingCase.isPresent()) {
            return toResponse(existingCase.get());
        }

        RecoveryCase recoveryCase =
                RecoveryCase.open(
                        payment,
                        payment.getMerchant()
                );

        RecoveryCase saved =
                recoveryCaseRepository.save(recoveryCase);

        auditService.record(
                saved.getCorrelationId(),
                "RECOVERY_CASE",
                saved.getId(),
                AuditEventType.RECOVERY_CASE_CREATED,
                Map.of(
                        "paymentId", payment.getId().toString(),
                        "source", "API"
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecoveryCaseResponse> list() {

        return recoveryCaseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecoveryCaseResponse get(UUID recoveryCaseId) {

        RecoveryCase recoveryCase =
                recoveryCaseRepository.findById(recoveryCaseId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Recovery case",
                                        recoveryCaseId
                                ));

        return toResponse(recoveryCase);
    }

    @Transactional(readOnly = true)
    public List<RecoveryDecisionResponse> decisions(
            UUID recoveryCaseId) {

        if (!recoveryCaseRepository.existsById(recoveryCaseId)) {
            throw new NotFoundException(
                    "Recovery case",
                    recoveryCaseId
            );
        }

        return recoveryDecisionRepository
                .findByRecoveryCaseIdOrderByCreatedAtAsc(recoveryCaseId)
                .stream()
                .map(this::toDecisionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditEvents(
            UUID recoveryCaseId) {

        RecoveryCase recoveryCase =
                recoveryCaseRepository.findById(recoveryCaseId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Recovery case",
                                        recoveryCaseId
                                ));

        return auditEventRepository
                .findByCorrelationIdOrderByOccurredAtAsc(
                        recoveryCase.getCorrelationId()
                )
                .stream()
                .map(this::toAuditResponse)
                .toList();
    }

    @Transactional
    public RecoveryCaseResponse createFromPaymentFailure(
            PaymentFailedEvent event) {

        Payment payment =
                paymentRepository.findById(event.paymentId())
                        .orElse(null);

        if (payment == null) {
            return null;
        }

        /*
         * Kafka events can be replayed.
         *
         * If a recovery case already exists for this payment,
         * return it instead of creating a duplicate.
         */
        var existingCase =
                recoveryCaseRepository.findByPaymentId(payment.getId());

        if (existingCase.isPresent()) {
            return toResponse(existingCase.get());
        }

        RecoveryCase recoveryCase =
                RecoveryCase.open(
                        payment,
                        payment.getMerchant()
                );

        RecoveryCase saved =
                recoveryCaseRepository.save(recoveryCase);

        auditService.record(
                saved.getCorrelationId(),
                "RECOVERY_CASE",
                saved.getId(),
                AuditEventType.RECOVERY_CASE_CREATED,
                Map.of(
                        "paymentId", payment.getId().toString(),
                        "source", "KAFKA"
                )
        );

        return toResponse(saved);
    }

    private RecoveryCaseResponse toResponse(
            RecoveryCase recoveryCase) {

        Payment payment = recoveryCase.getPayment();

        return new RecoveryCaseResponse(
                recoveryCase.getId(),
                payment.getId(),
                recoveryCase.getCorrelationId(),
                recoveryCase.getStatus(),
                recoveryCase.getRetryCount(),
                recoveryCase.getOpenedAt(),
                recoveryCase.getClosedAt()
        );
    }

    private RecoveryDecisionResponse toDecisionResponse(
            RecoveryDecision decision) {

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

    private AuditEventResponse toAuditResponse(
            AuditEvent event) {

        return new AuditEventResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getActor(),
                event.getPayload(),
                event.getOccurredAt()
        );
    }
}