package dev.portfolio.payments.payment.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="app.outbox.poll-ms=600000")
class PaymentApiIntegrationTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.1"));
    static { postgres.start(); kafka.start(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    @Autowired PaymentService service;
    @Autowired JdbcTemplate jdbc;

    @Test void createsPaymentAndOutboxInSameTransaction() throws Exception {
        var response=service.submit(new PaymentDtos.CreatePaymentRequest(new BigDecimal("12.50"), "GBP"));
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payments WHERE id=?",Integer.class,response.paymentId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE aggregate_id=?",Integer.class,response.paymentId())).isEqualTo(1);
    }
}
