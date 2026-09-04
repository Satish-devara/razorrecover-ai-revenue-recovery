package com.razorrecover.payment.provider;

import com.razorrecover.domain.enums.PaymentFailureReason;
import com.razorrecover.domain.enums.SimulationScenario;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RazorpayPaymentProvider implements PaymentProvider {

    private final RazorpayProperties properties;

    public RazorpayPaymentProvider(RazorpayProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentExecutionResult executeAttempt(PaymentProviderRequest request) {

        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "Razorpay provider is disabled. Set RAZORPAY_ENABLED=true to enable it."
            );
        }

        try {
            RazorpayClient client =
                    new RazorpayClient(
                            properties.getKeyId(),
                            properties.getKeySecret()
                    );

            JSONObject orderRequest = new JSONObject();
            orderRequest.put(
                    "amount",
                    new BigDecimal("1.00")
                            .multiply(BigDecimal.valueOf(100))
                            .intValue()
            );
            orderRequest.put("currency", "INR");
            orderRequest.put(
                    "receipt",
                    "recovery_" + UUID.randomUUID()
            );

            Order order = client.orders.create(orderRequest);

            return PaymentExecutionResult.succeeded();

        } catch (Exception exception) {

            return PaymentExecutionResult.failed(
                    PaymentFailureReason.TEMPORARY_NETWORK_ERROR
            );
        }
    }
}
