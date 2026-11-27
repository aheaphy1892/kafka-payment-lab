package dev.portfolio.payments.payment.processor.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class ProcessingRepository {

    private final JdbcTemplate jdbc;
    public ProcessingRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public boolean claim(UUID eventId) { return jdbc.update("INSERT INTO processed_events(id) VALUES (?) ON CONFLICT DO NOTHING",eventId)==1; }

    public void saveDecision(UUID paymentId,String status) { jdbc.update("INSERT INTO payment_decisions(payment_id,status) VALUES (?,?) ON CONFLICT (payment_id) DO NOTHING",paymentId,status); }

    public void saveOutbox(UUID eventId,UUID paymentId,String topic,String payload) { jdbc.update("INSERT INTO outbox(id,aggregate_id,topic,payload) VALUES (?,?,?,?::jsonb) ON CONFLICT DO NOTHING",eventId,paymentId,topic,payload); }
}
