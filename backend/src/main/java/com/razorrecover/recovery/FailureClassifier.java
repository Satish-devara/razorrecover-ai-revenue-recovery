package com.razorrecover.recovery;

import com.razorrecover.domain.enums.PaymentFailureReason;
import com.razorrecover.domain.enums.RecoveryAction;

import org.springframework.stereotype.Component;

@Component
public class FailureClassifier {

    public RecoveryAction classify(PaymentFailureReason reason) {

        if (reason == null) {
            return RecoveryAction.ESCALATE;
        }

        return switch (reason) {
            case TIMEOUT,
                 TEMPORARY_NETWORK_ERROR,
                 BANK_UNAVAILABLE ->
                    RecoveryAction.RETRY_PAYMENT;

            case PAYMENT_METHOD_ISSUE,
                 INSUFFICIENT_FUNDS,
                 ABANDONED ->
                    RecoveryAction.REQUEST_CUSTOMER_ACTION;

            case PERMANENT_FAILURE ->
                    RecoveryAction.ESCALATE;
        };
    }
}
