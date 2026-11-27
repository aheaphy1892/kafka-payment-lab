package dev.portfolio.payments.payment.processor.services;

import dev.portfolio.payments.contracts.PaymentEvents;
import dev.portfolio.payments.payment.processor.repository.ProcessingRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentProcessingService {

    private final ProcessingRepository repository;
    private final ObjectMapper mapper;

    public PaymentProcessingService(ProcessingRepository repository,ObjectMapper mapper) {
        this.repository=repository;this.mapper=mapper;
    }

    @Transactional
    public void process(PaymentEvents.Requested event) {
        if (!repository.claim(event.eventId())) return;
        repository.saveDecision(event.paymentId(),"APPROVED"); // deterministic simulation; no real payment transfer
        var result = new PaymentEvents.Processed(UUID.randomUUID(),event.paymentId(),"APPROVED",Instant.now());
        repository.saveOutbox(result.eventId(),event.paymentId(),PaymentEvents.PROCESSED,mapper.writeValueAsString(result));
    }
}
