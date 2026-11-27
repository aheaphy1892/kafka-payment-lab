package dev.portfolio.payments.payment.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class ProcessedEventRepository {

    private final JdbcTemplate jdbc;

    public ProcessedEventRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public boolean recordIfNew(UUID id) { return jdbc.update("INSERT INTO processed_events(id) VALUES (?) ON CONFLICT DO NOTHING",id)==1; }

}
