package dev.portfolio.payments.payment.processor;

import dev.portfolio.payments.contracts.PaymentEvents;
import dev.portfolio.payments.payment.processor.services.PaymentProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="app.outbox.poll-ms=600000")
class PaymentProcessorIntegrationTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.1"));
    static { postgres.start(); kafka.start(); }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    @Autowired
    PaymentProcessingService service;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void replayDoesNotCreateSecondDecisionOrOutboxEvent() throws Exception {
        var event=new PaymentEvents.Requested(UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("8.00"),"GBP",Instant.now());
        var record=new ConsumerRecord<String,String>(PaymentEvents.REQUESTED,0,1L,event.paymentId().toString(),mapper.writeValueAsString(event));
        service.process(event);
        service.process(event); // simulated replay after DB commit but before Kafka offset commit
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_decisions",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox",Integer.class)).isEqualTo(1);
    }
}
