package com.razorrecover.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentFailedEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentFailedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.PAYMENT_FAILED,
                event.paymentId().toString(),
                event
        );
    }
}
