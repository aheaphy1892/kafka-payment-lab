package dev.portfolio.payments.payment.api.metrics;

import dev.portfolio.payments.payment.api.repository.PaymentOutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Exposes the payment API's transactional outbox backlog to Micrometer.
 *
 * <p>These gauges query the API database when scraped. Keep the queries
 * inexpensive; the repository uses aggregate queries over the outbox table.</p>
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(
            MeterRegistry meterRegistry,
            PaymentOutboxRepository outboxRepository) {

        Gauge.builder("payments.outbox.pending", outboxRepository,
                        PaymentOutboxRepository::countPending)
                .description("Number of unpublished payment API outbox records")
                .register(meterRegistry);

        Gauge.builder("payments.outbox.oldest.pending.age.seconds", outboxRepository,
                        PaymentOutboxRepository::oldestPendingAgeSeconds)
                .description("Age in seconds of the oldest unpublished payment API outbox record")
                .register(meterRegistry);
    }
}
