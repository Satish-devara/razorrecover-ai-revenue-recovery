package com.razorrecover.domain.enums;

public enum RecoveryCaseStatus {
    OPEN, RETRY_PENDING, RECOVERED, ESCALATED, STOPPED;

    public boolean isTerminal() {
        return this == RECOVERED || this == ESCALATED || this == STOPPED;
    }
}
