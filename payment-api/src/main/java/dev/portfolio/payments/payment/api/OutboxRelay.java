package dev.portfolio.payments.payment.api;

import dev.portfolio.payments.payment.api.repository.PaymentOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(OutboxRelay.class);

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafka;
    private final MeterRegistry metrics;
    private final int batchSize;
    private final Duration leaseDuration;

    public OutboxRelay(
        PaymentOutboxRepository outboxRepository,
        KafkaTemplate<String, String> kafka,
        MeterRegistry metrics,
        @Value("${app.outbox.batch-size:50}") int batchSize,
        @Value("${app.outbox.lease-seconds:120}") long leaseSeconds) {

        this.outboxRepository = outboxRepository;
        this.kafka = kafka;
        this.metrics = metrics;
        this.batchSize = batchSize;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);

        metrics.gauge("outbox.pending", outboxRepository,
            PaymentOutboxRepository::countPending);
        metrics.gauge("outbox.oldest.pending.age", outboxRepository,
            PaymentOutboxRepository::oldestPendingAgeSeconds);
    }

    /**
     * Claims rows using a short database operation, then publishes outside the
     * claim transaction. A crash after Kafka acknowledges but before the row is
     * marked published can still cause a duplicate; consumers must be idempotent.
     */
    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    public void publish() {
        UUID claimToken = UUID.randomUUID();
        List<OutboxClaim> claims = outboxRepository.claimPendingBatch(
            claimToken, batchSize, leaseDuration);

        for (OutboxClaim claim : claims) {
            try {
                kafka.send(claim.topic(), claim.aggregateId(), claim.payload())
                    .get(10, TimeUnit.SECONDS);

                int updated = outboxRepository.markPublished(claim.id(), claimToken);
                if (updated != 1) {
                    // Kafka may already have accepted the event. Do not mark a row
                    // owned by another relay; downstream idempotency handles replay.
                    metrics.counter("outbox.publish.mark-failed").increment();
                    log.warn("Kafka accepted outbox event {}, but its claim was no longer owned",
                        claim.id());
                    break;
                }

                metrics.counter("outbox.publish.success").increment();
            } catch (Exception ex) {
                if (ex instanceof InterruptedException
                    || ex.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                try {
                    outboxRepository.releaseClaims(claimToken);
                } catch (Exception releaseFailure) {
                    ex.addSuppressed(releaseFailure);
                }

                metrics.counter("outbox.publish.failure").increment();
                log.warn("Outbox publish failed for {}", claim.id(), ex);
                break;
            }
        }
    }
}