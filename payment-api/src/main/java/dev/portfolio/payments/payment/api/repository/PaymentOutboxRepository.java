package dev.portfolio.payments.payment.api.repository;

import dev.portfolio.payments.payment.api.OutboxClaim;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 * PostgreSQL-backed repository for safely claiming and completing API outbox rows.
 *
 * <p>Rows are claimed in one short database statement. The transaction/row locks
 * used by PostgreSQL are not held while the caller publishes to Kafka.</p>
 */
@Repository
public class PaymentOutboxRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper;

    public PaymentOutboxRepository(ObjectMapper mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String CLAIM_SQL = """
        UPDATE outbox
           SET claim_token = ?,
               locked_until = now() + (? * interval '1 second')
         WHERE id IN (
             SELECT id
               FROM outbox
              WHERE published_at IS NULL
                AND (locked_until IS NULL OR locked_until < now())
              ORDER BY created_at, id
              LIMIT ?
              FOR UPDATE SKIP LOCKED
         )
        RETURNING id, aggregate_id, topic, payload::text AS payload
        """;

    /**
     * Claims up to {@code batchSize} unpublished rows for this relay invocation.
     * A claim becomes available to another relay after {@code leaseDuration}
     * expires, for example if this process crashes.
     */
    public List<OutboxClaim> claimPendingBatch(
            UUID claimToken,
            int batchSize,
            Duration leaseDuration) {

        if (claimToken == null) {
            throw new IllegalArgumentException("claimToken must not be null");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }

        long leaseSeconds = Math.max(1L, leaseDuration.toSeconds());

        return jdbcTemplate.query(
                CLAIM_SQL,
                (rs, rowNum) -> mapClaim(rs),
                claimToken,
                leaseSeconds,
                batchSize);
    }

    /**
     * Marks a row as published only if it is still owned by the supplied token.
     * Returns the number of rows updated (normally 1; 0 means the lease was lost).
     */
    public int markPublished(UUID outboxId, UUID claimToken) {
        return jdbcTemplate.update("""
                UPDATE outbox
                   SET published_at = now(),
                       claim_token = NULL,
                       locked_until = NULL
                 WHERE id = ?
                   AND claim_token = ?
                   AND published_at IS NULL
                """,
                outboxId,
                claimToken);
    }

    /** Releases all still-pending rows claimed by this invocation. */
    public int releaseClaims(UUID claimToken) {
        return jdbcTemplate.update("""
                UPDATE outbox
                   SET claim_token = NULL,
                       locked_until = NULL
                 WHERE claim_token = ?
                   AND published_at IS NULL
                """,
                claimToken);
    }

    public long countPending() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox WHERE published_at IS NULL",
                Long.class);
        return count == null ? 0L : count;
    }

    public double oldestPendingAgeSeconds() {
        Double age = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                    EXTRACT(EPOCH FROM (now() - min(created_at))),
                    0
                )
                  FROM outbox
                 WHERE published_at IS NULL
                """,
                Double.class);
        return age == null ? 0.0d : age;
    }

    public void save(UUID eventId, UUID paymentId, String topic, Object event) {
        jdbcTemplate.update("INSERT INTO outbox(id,aggregate_id,topic,payload) VALUES (?,?,?,?::jsonb)", eventId, paymentId, topic, mapper.writeValueAsString(event));
    }

    private static OutboxClaim mapClaim(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID aggregateId = rs.getObject("aggregate_id", UUID.class);
        return new OutboxClaim(
                id,
                aggregateId.toString(),
                rs.getString("topic"),
                rs.getString("payload"));
    }
}
