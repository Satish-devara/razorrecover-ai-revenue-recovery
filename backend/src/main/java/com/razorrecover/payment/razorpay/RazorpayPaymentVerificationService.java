package com.razorrecover.payment.razorpay;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.razorrecover.dto.RazorpayPaymentVerificationRequest;
import com.razorrecover.payment.provider.RazorpayProperties;
import com.razorrecover.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayPaymentVerificationService {

    private final RazorpayProperties properties;
    private final PaymentRepository paymentRepository;

    public RazorpayPaymentVerificationService(
            RazorpayProperties properties,
            PaymentRepository paymentRepository) {
        this.properties = properties;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void verify(
            com.razorrecover.domain.Payment payment,
            RazorpayPaymentVerificationRequest request) {

        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "Razorpay integration is disabled."
            );
        }

        try {
            RazorpayClient client = new RazorpayClient(
                    properties.getKeyId(),
                    properties.getKeySecret()
            );

            // ---------------------------------------------------------
            // 1. Fetch the Razorpay Order
            // ---------------------------------------------------------
            Order order = client.orders.fetch(
                    request.razorpayOrderId()
            );

            // Convert our payment amount from INR/Rupees to paise.
            long expectedAmount = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            long razorpayOrderAmount =
                    ((Number) order.get("amount")).longValue();

            // Verify that the Razorpay Order amount matches
            // the amount of our internal payment.
            if (razorpayOrderAmount != expectedAmount) {
                throw new IllegalArgumentException(
                        "Razorpay order amount does not match payment amount"
                );
            }

            // ---------------------------------------------------------
            // 2. Fetch the Razorpay Payment
            // ---------------------------------------------------------
            Payment razorpayPayment = client.payments.fetch(
                    request.razorpayPaymentId()
            );

            // ---------------------------------------------------------
            // 3. Verify payment amount
            // ---------------------------------------------------------
            long razorpayPaymentAmount =
                    ((Number) razorpayPayment.get("amount")).longValue();

            if (razorpayPaymentAmount != expectedAmount) {
                throw new IllegalArgumentException(
                        "Razorpay payment amount does not match payment amount"
                );
            }

            // ---------------------------------------------------------
            // 4. Verify payment belongs to supplied order
            // ---------------------------------------------------------
            String orderId = razorpayPayment.get("order_id");

            if (!request.razorpayOrderId().equals(orderId)) {
                throw new IllegalArgumentException(
                        "Razorpay payment does not belong to the supplied order"
                );
            }

            // ---------------------------------------------------------
            // 5. Verify payment was actually captured
            // ---------------------------------------------------------
            String paymentStatus = razorpayPayment.get("status");

            if (!"captured".equalsIgnoreCase(paymentStatus)) {
                throw new IllegalArgumentException(
                        "Razorpay payment is not captured"
                );
            }

            // ---------------------------------------------------------
            // 6. Verify Razorpay signature
            // ---------------------------------------------------------
            JSONObject verificationData = new JSONObject();

            verificationData.put(
                    "razorpay_order_id",
                    request.razorpayOrderId()
            );

            verificationData.put(
                    "razorpay_payment_id",
                    request.razorpayPaymentId()
            );

            verificationData.put(
                    "razorpay_signature",
                    request.razorpaySignature()
            );

            boolean valid = Utils.verifyPaymentSignature(
                    verificationData,
                    properties.getKeySecret()
            );

            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid Razorpay payment signature"
                );
            }

            // ---------------------------------------------------------
            // 7. Mark our internal payment as successful
            // ---------------------------------------------------------
            // This happens ONLY after all Razorpay verification checks
            // have passed.
            payment.applyAttemptResult(true, null);

            paymentRepository.save(payment);

        } catch (IllegalArgumentException exception) {

            // Expected validation/verification failures should be
            // returned as BAD_REQUEST by ApiExceptionHandler.
            throw exception;

        } catch (Exception exception) {

            // Razorpay SDK/API failures are converted into a controlled
            // API error instead of returning an uncontrolled 500.
            throw new IllegalArgumentException(
                    "Razorpay payment verification failed: "
                            + exception.getMessage()
            );
        }
    }
}