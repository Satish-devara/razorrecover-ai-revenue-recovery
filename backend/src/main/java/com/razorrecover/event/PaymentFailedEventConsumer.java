package com.razorrecover.event;

import com.razorrecover.recovery.RecoveryCaseService;
import com.razorrecover.recovery.RecoveryEngine;
import com.razorrecover.support.OperationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedEventConsumer {

    private final RecoveryCaseService recoveryCaseService;
    private final RecoveryEngine recoveryEngine;

    public PaymentFailedEventConsumer(
            RecoveryCaseService recoveryCaseService,
            RecoveryEngine recoveryEngine) {

        this.recoveryCaseService = recoveryCaseService;
        this.recoveryEngine = recoveryEngine;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${KAFKA_CONSUMER_GROUP:razorrecover-backend}"
    )
    public void consume(PaymentFailedEvent event) {

        var recoveryCase =
                recoveryCaseService.createFromPaymentFailure(event);

        if (recoveryCase == null) {
            return;
        }

        /*
         * Kafka may replay an older payment.failed event.
         *
         * If the recovery case is already terminal, there is
         * nothing left to recover. Treat the event as successfully
         * handled instead of throwing an exception and causing
         * Kafka to retry the same message.
         */
        if (recoveryCase.status().isTerminal()) {
            return;
        }

        recoveryEngine.evaluate(
                recoveryCase.id(),
                OperationContext.from(null)
        );
    }
}