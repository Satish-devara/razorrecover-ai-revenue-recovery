package com.razorrecover.payment;

import com.razorrecover.audit.AuditService;
import com.razorrecover.domain.Merchant;
import com.razorrecover.domain.Payment;
import com.razorrecover.domain.PaymentAttempt;
import com.razorrecover.domain.enums.AttemptTriggerType;
import com.razorrecover.domain.enums.AuditEventType;
import com.razorrecover.domain.enums.PaymentStatus;
import com.razorrecover.dto.CreatePaymentRequest;
import com.razorrecover.dto.PaymentAttemptResponse;
import com.razorrecover.dto.PaymentResponse;
import com.razorrecover.dto.SimulateFailureRequest;
import com.razorrecover.event.PaymentEventProducer;
import com.razorrecover.event.PaymentFailedEvent;
import com.razorrecover.payment.provider.PaymentExecutionResult;
import com.razorrecover.payment.provider.PaymentProvider;
import com.razorrecover.payment.provider.PaymentProviderRequest;
import com.razorrecover.payment.razorpay.RazorpayOrderService;
import com.razorrecover.payment.razorpay.RazorpayPaymentVerificationService;
import com.razorrecover.dto.RazorpayPaymentVerificationRequest;
import com.razorrecover.repository.MerchantRepository;
import com.razorrecover.repository.PaymentAttemptRepository;
import com.razorrecover.repository.PaymentRepository;
import com.razorrecover.support.InvalidStateException;
import com.razorrecover.support.NotFoundException;
import com.razorrecover.support.OperationContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String DEMO_MERCHANT_REFERENCE = "demo-merchant";

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProvider paymentProvider;
    private final AuditService auditService;
    private final PaymentEventProducer paymentEventProducer;
    private final RazorpayOrderService razorpayOrderService;
    private final RazorpayPaymentVerificationService razorpayPaymentVerificationService;

    public PaymentService(
            MerchantRepository merchantRepository,
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentProvider paymentProvider,
            AuditService auditService,
            PaymentEventProducer paymentEventProducer,
            RazorpayOrderService razorpayOrderService,
            RazorpayPaymentVerificationService razorpayPaymentVerificationService) {

        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentProvider = paymentProvider;
        this.auditService = auditService;
        this.paymentEventProducer = paymentEventProducer;
        this.razorpayOrderService = razorpayOrderService;
        this.razorpayPaymentVerificationService = razorpayPaymentVerificationService;
    }

    @Transactional
    public PaymentResponse create(
            CreatePaymentRequest request,
            OperationContext context) {

        Merchant merchant = merchantRepository
                .findByExternalReference(DEMO_MERCHANT_REFERENCE)
                .orElseGet(() ->
                        merchantRepository.save(
                                Merchant.createDemoMerchant()
                        )
                );

        String providerPaymentId =
                request.providerPaymentId() == null
                        || request.providerPaymentId().isBlank()
                        ? "sim_" + UUID.randomUUID()
                        : request.providerPaymentId();

        Payment payment = paymentRepository.save(
                Payment.create(
                        merchant,
                        providerPaymentId,
                        request.amount(),
                        request.currency(),
                        request.scenario()
                )
        );

        auditService.record(
                correlationId(payment),
                "PAYMENT",
                payment.getId(),
                AuditEventType.PAYMENT_CREATED,
                payload(
                        "scenario",
                        request.scenario().name(),
                        context
                )
        );

        if (!Boolean.FALSE.equals(request.processImmediately())) {
            executeInitialAttempt(payment, context);
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public com.razorrecover.payment.razorpay.RazorpayOrderService.RazorpayOrderResponse
            createRazorpayOrder(UUID paymentId) {

        Payment payment = findPayment(paymentId);

        return razorpayOrderService.createOrder(payment);
    }

    @Transactional
    public PaymentResponse verifyRazorpayPayment(
            UUID paymentId,
            RazorpayPaymentVerificationRequest request) {

        Payment payment = findPayment(paymentId);

        razorpayPaymentVerificationService.verify(payment, request);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID paymentId) {
        return toResponse(findPayment(paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list() {
        return paymentRepository
                .findAll()
                .stream()
                .map(PaymentService::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse simulateFailure(
            UUID paymentId,
            SimulateFailureRequest request,
            OperationContext context) {

        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidStateException(
                    "Only a pending payment can be simulated as failed"
            );
        }

        payment.setSimulationScenario(request.scenario());

        PaymentExecutionResult result =
                execute(
                        payment,
                        1,
                        AttemptTriggerType.INITIAL
                );

        if (result.successful()) {
            throw new InvalidStateException(
                    "The selected scenario does not produce an initial failure"
            );
        }

        persistAttemptOutcome(
                payment,
                1,
                AttemptTriggerType.INITIAL,
                result,
                context,
                false
        );

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse retry(
            UUID paymentId,
            OperationContext context) {

        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException(
                    "Only a failed payment can be retried"
            );
        }

        int attemptNumber =
                paymentAttemptRepository.countByPaymentId(paymentId) + 1;

        auditService.record(
                correlationId(payment),
                "PAYMENT",
                payment.getId(),
                AuditEventType.PAYMENT_RETRY_ATTEMPTED,
                payload(
                        "attemptNumber",
                        attemptNumber,
                        context
                )
        );

        PaymentExecutionResult result =
                execute(
                        payment,
                        attemptNumber,
                        AttemptTriggerType.AUTOMATIC_RECOVERY
                );

        persistAttemptOutcome(
                payment,
                attemptNumber,
                AttemptTriggerType.AUTOMATIC_RECOVERY,
                result,
                context,
                true
        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> attempts(UUID paymentId) {

        findPayment(paymentId);

        return paymentAttemptRepository
                .findByPaymentIdOrderByAttemptNumberAsc(paymentId)
                .stream()
                .map(attempt ->
                        new PaymentAttemptResponse(
                                attempt.getId(),
                                attempt.getAttemptNumber(),
                                attempt.getTriggerType(),
                                attempt.getStatus(),
                                attempt.getFailureReason(),
                                attempt.getAttemptedAt()
                        )
                )
                .toList();
    }

    private void executeInitialAttempt(
            Payment payment,
            OperationContext context) {

        PaymentExecutionResult result =
                execute(
                        payment,
                        1,
                        AttemptTriggerType.INITIAL
                );

        persistAttemptOutcome(
                payment,
                1,
                AttemptTriggerType.INITIAL,
                result,
                context,
                false
        );
    }

    private PaymentExecutionResult execute(
            Payment payment,
            int attemptNumber,
            AttemptTriggerType triggerType) {

        return paymentProvider.executeAttempt(
                new PaymentProviderRequest(
                        payment.getProviderPaymentId(),
                        payment.getSimulationScenario(),
                        attemptNumber,
                        triggerType
                )
        );
    }

    private void persistAttemptOutcome(
            Payment payment,
            int attemptNumber,
            AttemptTriggerType triggerType,
            PaymentExecutionResult result,
            OperationContext context,
            boolean retry) {

        // 1. Save payment attempt
        paymentAttemptRepository.save(
                PaymentAttempt.create(
                        payment,
                        attemptNumber,
                        triggerType,
                        result
                )
        );

        // 2. Update payment status
        payment.applyAttemptResult(
                result.successful(),
                result.failureReason()
        );

        paymentRepository.save(payment);

        // 3. Record audit event
        AuditEventType eventType =
                result.successful()
                        ? (retry
                        ? AuditEventType.PAYMENT_RECOVERED
                        : AuditEventType.PAYMENT_SUCCEEDED)
                        : (retry
                        ? AuditEventType.PAYMENT_RETRY_FAILED
                        : AuditEventType.PAYMENT_FAILED);

        auditService.record(
                correlationId(payment),
                "PAYMENT",
                payment.getId(),
                eventType,
                payload(
                        "attemptNumber",
                        attemptNumber,
                        context
                )
        );

        // 4. Publish Kafka event whenever payment fails
        if (!result.successful()) {

            PaymentFailedEvent event =
                    new PaymentFailedEvent(
                            payment.getId(),
                            payment.getMerchant().getId(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            result.failureReason() != null
                                    ? result.failureReason().name()
                                    : null,
                            payment.getSimulationScenario() != null
                                    ? payment.getSimulationScenario().name()
                                    : null,
                            payment.getFailedAt(),
                            correlationId(payment)
                    );

            paymentEventProducer.publishPaymentFailed(event);
        }
    }

    private Payment findPayment(UUID paymentId) {

        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Payment",
                                paymentId
                        )
                );
    }

    private static String correlationId(Payment payment) {
        return payment.getId().toString();
    }

    private static Map<String, Object> payload(
            String key,
            Object value,
            OperationContext context) {

        if (context.idempotencyKey() == null
                || context.idempotencyKey().isBlank()) {

            return Map.of(key, value);
        }

        return Map.of(
                key,
                value,
                "idempotencyKey",
                context.idempotencyKey()
        );
    }

    private static PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getProviderPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getSimulationScenario(),
                payment.getFailedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}