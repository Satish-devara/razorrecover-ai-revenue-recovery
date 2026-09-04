package com.razorrecover.domain.enums;

/** Fixed outcome sequences used by the local payment simulator. */
public enum SimulationScenario {
    TEMPORARY_NETWORK_FAILURE,
    TIMEOUT,
    BANK_UNAVAILABLE,
    PAYMENT_METHOD_FAILURE,
    PERMANENT_FAILURE,
    SUCCESS,
    RETRY_THEN_SUCCESS,
    RETRY_THEN_FAILURE
}
