package dev.portfolio.payments.payment.api;

import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class PaymentOutboxRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PaymentOutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc=jdbc; this.mapper=mapper;
    }

    public void save(UUID eventId, UUID paymentId, String topic, Object event) {
        jdbc.update("INSERT INTO outbox(id,aggregate_id,topic,payload) VALUES (?,?,?,?::jsonb)", eventId, paymentId, topic, mapper.writeValueAsString(event));
    }

}
