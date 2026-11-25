package dev.portfolio.payments.audit.service;
import dev.portfolio.payments.contracts.PaymentEvents;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;
    public AuditRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void append(PaymentEvents.Requested event,int partition,long offset) {
        jdbc.update("INSERT INTO audit_log(event_id,payment_id,amount,currency,partition_id,offset_id) VALUES (?,?,?,?,?,?) ON CONFLICT (event_id) DO NOTHING",event.eventId(),event.paymentId(),event.amount(),event.currency(),partition,offset);
    }
}
