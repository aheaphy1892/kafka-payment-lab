package dev.portfolio.payments.payment.processor;

import dev.portfolio.payments.contracts.PaymentEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "app.outbox.poll-ms=600000")
class PaymentConsumerRecoveryIntegrationTest {

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

    @Autowired KafkaListenerEndpointRegistry listeners;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void consumesBacklogAfterListenerContainerRestart() throws Exception {
        var container = listeners.getListenerContainer("payment-processing-listener");
        assertThat(container).isNotNull();
        ContainerTestUtils.waitForAssignment(container, 1); // ensures partition is assigned before stopping
        container.stop();

        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        var event = new PaymentEvents.Requested(eventId, paymentId, new BigDecimal("19.95"), "GBP", Instant.now());
        kafkaTemplate.send(PaymentEvents.REQUESTED, paymentId.toString(), mapper.writeValueAsString(event)).get();

        container.start();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_decisions WHERE payment_id=?", Integer.class, paymentId))
                    .isOne());
    }
}
