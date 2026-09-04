package com.razorrecover.payment.provider;

import com.razorrecover.domain.enums.PaymentFailureReason;
import com.razorrecover.domain.enums.SimulationScenario;
import org.springframework.stereotype.Component;

/** A stateless and reproducible provider implementation for local demos and tests. */
@Component
public class SimulationPaymentProvider implements PaymentProvider {

    @Override
    public PaymentExecutionResult executeAttempt(PaymentProviderRequest request) {
        return switch (request.scenario()) {
            case SUCCESS -> PaymentExecutionResult.succeeded();
            case TEMPORARY_NETWORK_FAILURE -> retrySucceeds(
                    request.attemptNumber(), PaymentFailureReason.TEMPORARY_NETWORK_ERROR);
            case TIMEOUT -> retrySucceeds(request.attemptNumber(), PaymentFailureReason.TIMEOUT);
            case RETRY_THEN_SUCCESS -> retrySucceeds(request.attemptNumber(), PaymentFailureReason.TIMEOUT);
            case BANK_UNAVAILABLE -> PaymentExecutionResult.failed(PaymentFailureReason.BANK_UNAVAILABLE);
            case PAYMENT_METHOD_FAILURE -> PaymentExecutionResult.failed(PaymentFailureReason.PAYMENT_METHOD_ISSUE);
            case PERMANENT_FAILURE -> PaymentExecutionResult.failed(PaymentFailureReason.PERMANENT_FAILURE);
            case RETRY_THEN_FAILURE -> request.attemptNumber() == 1
                    ? PaymentExecutionResult.failed(PaymentFailureReason.TIMEOUT)
                    : PaymentExecutionResult.failed(PaymentFailureReason.PERMANENT_FAILURE);
        };
    }

    private PaymentExecutionResult retrySucceeds(int attemptNumber, PaymentFailureReason firstFailure) {
        return attemptNumber == 1
                ? PaymentExecutionResult.failed(firstFailure)
                : PaymentExecutionResult.succeeded();
    }
}
