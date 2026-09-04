package com.razorrecover.support;

/** Carries the future idempotency boundary without adding a cache dependency yet. */
public record OperationContext(String idempotencyKey) {
    public static OperationContext from(String idempotencyKey) {
        return new OperationContext(idempotencyKey);
    }
}
