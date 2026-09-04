package com.razorrecover.api;

import com.razorrecover.dto.CreatePaymentRequest;
import com.razorrecover.dto.PaymentAttemptResponse;
import com.razorrecover.dto.RazorpayPaymentVerificationRequest;
import com.razorrecover.dto.PaymentResponse;
import com.razorrecover.dto.SimulateFailureRequest;
import com.razorrecover.payment.PaymentService;
import com.razorrecover.payment.razorpay.RazorpayOrderService;
import com.razorrecover.support.OperationContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request, OperationContext.from(idempotencyKey)));
    }

    @PostMapping("/{paymentId}/razorpay-order")
    public RazorpayOrderService.RazorpayOrderResponse createRazorpayOrder(
            @PathVariable UUID paymentId) {

        return paymentService.createRazorpayOrder(paymentId);
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return paymentService.list();
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable UUID paymentId) {
        return paymentService.get(paymentId);
    }

    @PostMapping("/{paymentId}/simulate-failure")
    public PaymentResponse simulateFailure(
            @PathVariable UUID paymentId,
            @Valid @RequestBody SimulateFailureRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return paymentService.simulateFailure(paymentId, request, OperationContext.from(idempotencyKey));
    }

    @PostMapping("/{paymentId}/retries")
    public PaymentResponse retry(
            @PathVariable UUID paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return paymentService.retry(paymentId, OperationContext.from(idempotencyKey));
    }

    @PostMapping("/{paymentId}/razorpay/verify")
    public PaymentResponse verifyRazorpayPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RazorpayPaymentVerificationRequest request) {

        return paymentService.verifyRazorpayPayment(paymentId, request);
    }

    @GetMapping("/{paymentId}/attempts")
    public List<PaymentAttemptResponse> attempts(@PathVariable UUID paymentId) {
        return paymentService.attempts(paymentId);
    }
}
