package dev.portfolio.payments.payment.api;

import dev.portfolio.payments.contracts.PaymentEvents;
import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultConsumer {

    private final PaymentService service;
    private final ObjectMapper mapper;

    public PaymentResultConsumer(PaymentService service,ObjectMapper mapper) {
        this.service=service; this.mapper=mapper;
    }

    @KafkaListener(topics=PaymentEvents.PROCESSED, groupId="payment-api-results")
    public void receive(String payload) {
        service.applyResult(mapper.readValue(payload, PaymentEvents.Processed.class));
    }
}
