package com.razorrecover.payment.razorpay;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorrecover.domain.Payment;
import com.razorrecover.payment.provider.RazorpayProperties;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayOrderService {

    private final RazorpayProperties properties;

    public RazorpayOrderService(RazorpayProperties properties) {
        this.properties = properties;
    }

    public RazorpayOrderResponse createOrder(Payment payment) {
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

            long amountInPaise = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", payment.getCurrency());
            orderRequest.put(
                    "receipt",
                    "rr_" + payment.getId().toString().replace("-", "")
            );

            Order order = client.orders.create(orderRequest);

            return new RazorpayOrderResponse(
                    order.get("id"),
                    amountInPaise,
                    payment.getCurrency(),
                    properties.getKeyId()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to create Razorpay order",
                    exception
            );
        }
    }

    public record RazorpayOrderResponse(
            String orderId,
            long amount,
            String currency,
            String keyId
    ) {}
}
