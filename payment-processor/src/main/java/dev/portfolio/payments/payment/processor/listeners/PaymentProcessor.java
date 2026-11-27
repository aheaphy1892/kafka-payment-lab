package dev.portfolio.payments.payment.processor.listeners;

import dev.portfolio.payments.contracts.PaymentEvents;
import dev.portfolio.payments.payment.processor.services.PaymentProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentProcessor {

    private final PaymentProcessingService service;
    private final ObjectMapper mapper;

    public PaymentProcessor(PaymentProcessingService service,ObjectMapper mapper) {
        this.service=service;this.mapper=mapper;
    }

    @KafkaListener(
        id = "payment-processing-listener",
        topics = PaymentEvents.REQUESTED,
        groupId = "payment-processing"
    )
    public void receive(ConsumerRecord<String,String> record) {
        service.process(mapper.readValue(record.value(),PaymentEvents.Requested.class));
    }

}
