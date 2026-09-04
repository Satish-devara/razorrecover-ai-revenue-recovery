package com.razorrecover.payment.provider;

import com.razorrecover.domain.enums.PaymentFailureReason;

public record PaymentExecutionResult(boolean successful, PaymentFailureReason failureReason) {

    public static PaymentExecutionResult succeeded() {
        return new PaymentExecutionResult(true, null);
    }

    public static PaymentExecutionResult failed(PaymentFailureReason reason) {
        return new PaymentExecutionResult(false, reason);
    }
}
