package dev.portfolio.payments.payment.processor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka) { this.jdbc=jdbc; this.kafka=kafka; }

    // Intentionally at-least-once: a crash after Kafka acknowledgement but before UPDATE
    // can republish. Downstream consumers must deduplicate by eventId.
    @Scheduled(fixedDelayString="${app.outbox.poll-ms:1000}")
    public void publish() {
        var rows = jdbc.queryForList("SELECT id,aggregate_id,topic,payload::text AS payload FROM outbox WHERE published_at IS NULL ORDER BY created_at LIMIT 50");
        for (var row : rows) {
            UUID id=(UUID)row.get("id");
            try {
                kafka.send((String)row.get("topic"), row.get("aggregate_id").toString(), (String)row.get("payload"))
                        .get(10, TimeUnit.SECONDS);
                jdbc.update("UPDATE outbox SET published_at=now() WHERE id=? AND published_at IS NULL", id);
            } catch (Exception ex) {
                // Retain the outbox row for the next poll; surface the failure in logs.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Outbox publish failed for {}", id, ex);
                break;
            }
        }
    }
}
