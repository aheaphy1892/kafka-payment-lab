package dev.portfolio.payments.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentEvents {

    private PaymentEvents() {}

    public static final String REQUESTED = "payment.requested.v1";
    public static final String PROCESSED = "payment.processed.v1";

    public record Requested(UUID eventId, UUID paymentId, BigDecimal amount, String currency, Instant occurredAt) {}
    public record Processed(UUID eventId, UUID paymentId, String status, Instant occurredAt) {}
}
