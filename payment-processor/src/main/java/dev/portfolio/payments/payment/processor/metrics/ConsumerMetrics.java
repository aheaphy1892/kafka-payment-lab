package dev.portfolio.payments.payment.processor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Counters for the payment processor's Kafka consumer.
 *
 * <p>Call the relevant method from the listener/processing path so the metric
 * reflects the outcome of handling each record. Do not use payment IDs,
 * event IDs, topic partitions or exception messages as tags: they create
 * unbounded metric cardinality.</p>
 */
@Component
public class ConsumerMetrics {

    private final Counter processed;
    private final Counter failed;
    private final Counter duplicates;

    public ConsumerMetrics(MeterRegistry meterRegistry) {
        this.processed = Counter.builder("payments.consumer.records")
                .description("Payment request records successfully handled by the processor")
                .tag("application", "payment-processor")
                .tag("outcome", "processed")
                .register(meterRegistry);

        this.failed = Counter.builder("payments.consumer.records")
                .description("Payment request records that failed during processor handling")
                .tag("application", "payment-processor")
                .tag("outcome", "failed")
                .register(meterRegistry);

        this.duplicates = Counter.builder("payments.consumer.records")
                .description("Duplicate payment request records ignored by idempotent processing")
                .tag("application", "payment-processor")
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
