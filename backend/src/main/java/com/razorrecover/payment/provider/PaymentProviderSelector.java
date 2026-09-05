package com.razorrecover.payment.provider;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class PaymentProviderSelector implements PaymentProvider {

    private final RazorpayProperties properties;
    private final SimulationPaymentProvider simulationProvider;
    private final RazorpayPaymentProvider razorpayProvider;

    public PaymentProviderSelector(
            RazorpayProperties properties,
            SimulationPaymentProvider simulationProvider,
            RazorpayPaymentProvider razorpayProvider) {
        this.properties = properties;
        this.simulationProvider = simulationProvider;
        this.razorpayProvider = razorpayProvider;
    }

    @Override
    public PaymentExecutionResult executeAttempt(PaymentProviderRequest request) {

        // Simulation scenarios must always use the deterministic simulator.
        if (request.scenario() != null) {
            return simulationProvider.executeAttempt(request);
        }

        // Real Razorpay execution is used only when no simulation scenario is provided.
        if (properties.isEnabled()) {
            return razorpayProvider.executeAttempt(request);
        }

        return simulationProvider.executeAttempt(request);
    }
}
