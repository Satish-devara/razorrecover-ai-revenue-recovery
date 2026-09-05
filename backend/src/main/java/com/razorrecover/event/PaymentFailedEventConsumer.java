package com.razorrecover.event;

import com.razorrecover.recovery.RecoveryCaseService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedEventConsumer {

    private final RecoveryCaseService recoveryCaseService;

    public PaymentFailedEventConsumer(
            RecoveryCaseService recoveryCaseService) {

        this.recoveryCaseService = recoveryCaseService;
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
         * The recovery case is created here, but the actual
         * recovery decision is delegated to the AI recovery
         * service and then validated by the Java safety layer.
         */
        if (recoveryCase.status().isTerminal()) {
            return;
        }
    }
}