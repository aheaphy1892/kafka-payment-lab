package dev.portfolio.payments.payment.processor.metrics;

import dev.portfolio.payments.payment.processor.repository.PaymentOutBoxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Exposes the payment processor's result-event outbox backlog to Micrometer.
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(
            MeterRegistry meterRegistry,
            PaymentOutBoxRepository outboxRepository) {

        Gauge.builder("payments.outbox.pending", outboxRepository,
                        PaymentOutBoxRepository::countPending)
                .description("Number of unpublished payment processor outbox records")
                .register(meterRegistry);

        Gauge.builder("payments.outbox.oldest.pending.age.seconds", outboxRepository,
                        PaymentOutBoxRepository::oldestPendingAgeSeconds)
                .description("Age in seconds of the oldest unpublished payment processor outbox record")
                .register(meterRegistry);
    }
}
