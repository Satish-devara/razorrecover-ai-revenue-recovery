package com.razorrecover.payment.provider;

import com.razorrecover.domain.enums.AttemptTriggerType;
import com.razorrecover.domain.enums.SimulationScenario;

/** Provider-neutral request. A future Razorpay adapter can implement this contract. */
public record PaymentProviderRequest(
        String providerPaymentId,
        SimulationScenario scenario,
        int attemptNumber,
        AttemptTriggerType triggerType) {
}
