package dev.portfolio.payments.audit.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Counters for records handled by the independent audit Kafka consumer group.
 *
 * <p>Call these methods from the audit listener's success and failure paths.
 * Tags are deliberately low-cardinality and must not contain payment IDs,
 * event IDs, offsets or exception messages.</p>
 */
@Component
public class AuditConsumerMetrics {

    private final Counter processed;
    private final Counter failed;
    private final Counter duplicates;

    public AuditConsumerMetrics(MeterRegistry meterRegistry) {
        this.processed = Counter.builder("payments.consumer.records")
                .description("Payment request records successfully written to the audit log")
                .tag("application", "audit-service")
                .tag("outcome", "processed")
                .register(meterRegistry);

        this.failed = Counter.builder("payments.consumer.records")
                .description("Payment request records that failed during audit handling")
                .tag("application", "audit-service")
                .tag("outcome", "failed")
                .register(meterRegistry);

        this.duplicates = Counter.builder("payments.consumer.records")
                .description("Duplicate payment request records ignored by the audit consumer")
                .tag("application", "audit-service")
                .tag("outcome", "duplicate")
                .register(meterRegistry);
    }

    public void recordProcessed() {
        processed.increment();
    }

    public void recordFailure() {
        failed.increment();
    }

    public void recordDuplicate() {
        duplicates.increment();
    }
}
