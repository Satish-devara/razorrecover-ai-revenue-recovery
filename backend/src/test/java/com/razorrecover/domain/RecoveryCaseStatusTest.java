package com.razorrecover.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.razorrecover.domain.enums.RecoveryCaseStatus;
import org.junit.jupiter.api.Test;

class RecoveryCaseStatusTest {

    @Test
    void onlyCompletedOutcomesAreTerminal() {
        assertThat(RecoveryCaseStatus.OPEN.isTerminal()).isFalse();
        assertThat(RecoveryCaseStatus.RETRY_PENDING.isTerminal()).isFalse();
        assertThat(RecoveryCaseStatus.RECOVERED.isTerminal()).isTrue();
        assertThat(RecoveryCaseStatus.ESCALATED.isTerminal()).isTrue();
        assertThat(RecoveryCaseStatus.STOPPED.isTerminal()).isTrue();
    }
}
