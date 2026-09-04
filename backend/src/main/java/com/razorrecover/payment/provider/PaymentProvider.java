package com.razorrecover.payment.provider;

/**
 * Controlled payment-provider boundary. It reports an attempt outcome only;
 * persistence and recovery-state transitions remain in the Spring service.
 */
public interface PaymentProvider {
    PaymentExecutionResult executeAttempt(PaymentProviderRequest request);
}
