package dev.portfolio.payments.audit.service;

import dev.portfolio.payments.contracts.PaymentEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuditConsumer {

    private final AuditService service;
    private final ObjectMapper mapper;

    public AuditConsumer(AuditService service,ObjectMapper mapper) { this.service=service;this.mapper=mapper; }

    @KafkaListener(topics=PaymentEvents.REQUESTED,groupId="payment-audit")
    public void receive(ConsumerRecord<String,String> record) {
        service.record(mapper.readValue(record.value(),PaymentEvents.Requested.class),record.partition(),record.offset());
    }
}
