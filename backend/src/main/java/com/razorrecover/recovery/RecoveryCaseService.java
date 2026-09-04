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
import com.razorrecover.repository.AuditEventRepository;
import com.razorrecover.repository.PaymentRepository;
import com.razorrecover.repository.RecoveryCaseRepository;
import com.razorrecover.repository.RecoveryDecisionRepository;
import com.razorrecover.support.InvalidStateException;
import com.razorrecover.support.NotFoundException;
import com.razorrecover.support.OperationContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryCaseService {

    private final PaymentRepository paymentRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryDecisionRepository recoveryDecisionRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;

    public RecoveryCaseService(
            PaymentRepository paymentRepository,
            RecoveryCaseRepository recoveryCaseRepository,
            RecoveryDecisionRepository recoveryDecisionRepository,
            AuditEventRepository auditEventRepository,
            AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryDecisionRepository = recoveryDecisionRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RecoveryCaseResponse open(CreateRecoveryCaseRequest request, OperationContext context) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new NotFoundException("Payment", request.paymentId()));
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException("A recovery case can only be opened for a failed payment");
        }
        if (recoveryCaseRepository.findByPaymentId(payment.getId()).isPresent()) {
            throw new InvalidStateException("A recovery case already exists for this payment");
        }
        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.open(payment, payment.getMerchant()));
        auditService.record(recoveryCase.getCorrelationId(), "RECOVERY_CASE", recoveryCase.getId(),
                AuditEventType.RECOVERY_CASE_CREATED, payload(context));
        return toResponse(recoveryCase);
    }

    @Transactional(readOnly = true)
    public RecoveryCaseResponse get(UUID recoveryCaseId) {
        return toResponse(findCase(recoveryCaseId));
    }

    @Transactional(readOnly = true)
    public List<RecoveryCaseResponse> list() {
        return recoveryCaseRepository.findAll().stream().map(RecoveryCaseService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RecoveryDecisionResponse> decisions(UUID recoveryCaseId) {
        findCase(recoveryCaseId);
        return recoveryDecisionRepository.findByRecoveryCaseIdOrderByCreatedAtAsc(recoveryCaseId).stream()
                .map(RecoveryCaseService::toDecisionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditEvents(UUID recoveryCaseId) {
        RecoveryCase recoveryCase = findCase(recoveryCaseId);
        return auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(recoveryCase.getCorrelationId()).stream()
                .map(RecoveryCaseService::toAuditResponse)
                .toList();
    }

    private RecoveryCase findCase(UUID recoveryCaseId) {
        return recoveryCaseRepository.findById(recoveryCaseId)
                .orElseThrow(() -> new NotFoundException("Recovery case", recoveryCaseId));
    }

    private static Map<String, Object> payload(OperationContext context) {
        if (context.idempotencyKey() == null || context.idempotencyKey().isBlank()) {
            return Map.of("source", "API");
        }
        return Map.of("source", "API", "idempotencyKey", context.idempotencyKey());
    }

    private static RecoveryCaseResponse toResponse(RecoveryCase recoveryCase) {
        return new RecoveryCaseResponse(recoveryCase.getId(), recoveryCase.getPayment().getId(),
                recoveryCase.getCorrelationId(), recoveryCase.getStatus(), recoveryCase.getRetryCount(),
                recoveryCase.getOpenedAt(), recoveryCase.getClosedAt());
    }

    private static RecoveryDecisionResponse toDecisionResponse(RecoveryDecision decision) {
        return new RecoveryDecisionResponse(decision.getId(), decision.getRecommendedAction(), decision.getFinalAction(),
                decision.getConfidence(), decision.getExplanation(), decision.getSafetyCheckSummary(), decision.getOutcome(),
                decision.getCreatedAt());
    }

    private static AuditEventResponse toAuditResponse(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getAggregateType(), event.getAggregateId(), event.getEventType(),
                event.getActor(), event.getPayload(), event.getOccurredAt());
    }
}
