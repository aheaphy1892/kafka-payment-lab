package dev.portfolio.payments.payment.api.services;

import dev.portfolio.payments.contracts.PaymentEvents;

import dev.portfolio.payments.payment.api.repository.specifications.PaymentSpecifications;
import dev.portfolio.payments.payment.api.dto.PaymentDtos;
import dev.portfolio.payments.payment.api.dto.PaymentDtos.PaymentResponse;
import dev.portfolio.payments.payment.api.entities.Payment;
import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import dev.portfolio.payments.payment.api.repository.PaymentOutboxRepository;
import dev.portfolio.payments.payment.api.repository.PaymentRepository;
import dev.portfolio.payments.payment.api.repository.ProcessedEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final PaymentOutboxRepository outbox;
    private final ProcessedEventRepository processed;

    public PaymentService(PaymentRepository payments, PaymentOutboxRepository outbox, ProcessedEventRepository processed) {
        this.payments=payments; this.outbox=outbox; this.processed=processed;
    }

    @Transactional
    public PaymentDtos.PaymentResponse submit(PaymentDtos.CreatePaymentRequest request) {
        var payment = payments.save(new Payment(UUID.randomUUID(), request.amount(), request.currency()));
        var event = new PaymentEvents.Requested(UUID.randomUUID(), payment.getId(), payment.getAmount(), payment.getCurrency(), Instant.now());
        outbox.save(event.eventId(), event.paymentId(), PaymentEvents.REQUESTED, event);
        return PaymentDtos.PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse get(UUID id) {
        return payments.findById(id).map(PaymentDtos.PaymentResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> search(PaymentStatus status, String currency, Instant from, Instant to, Pageable pageable) {
        return payments.findAll(PaymentSpecifications.filtered(status, currency, from, to), pageable)
            .map(PaymentDtos.PaymentResponse::from);
    }

    @Transactional
    public void applyResult(PaymentEvents.Processed event) {
        if (!processed.recordIfNew(event.eventId())) return;
        var payment = payments.findById(event.paymentId())
            .orElseThrow(() -> new IllegalStateException("Unknown payment: " + event.paymentId()));
        payment.applyResult(PaymentStatus.valueOf(event.status()));
    }
}
