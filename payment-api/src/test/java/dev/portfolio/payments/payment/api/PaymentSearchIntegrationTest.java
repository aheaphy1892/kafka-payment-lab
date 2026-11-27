package dev.portfolio.payments.payment.api;

import dev.portfolio.payments.payment.api.dto.PaymentDtos;
import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import dev.portfolio.payments.payment.api.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="app.outbox.poll-ms=600000")
class PaymentSearchIntegrationTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.1"));
    static { postgres.start(); kafka.start(); }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);
        r.add("spring.datasource.username",postgres::getUsername);
        r.add("spring.datasource.password",postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers",kafka::getBootstrapServers);
    }

    @Autowired
    PaymentService service;

    @Test void filtersAndPagesPayments() {
        service.submit(new PaymentDtos.CreatePaymentRequest(new BigDecimal("12.50"),"GBP"));
        service.submit(new PaymentDtos.CreatePaymentRequest(new BigDecimal("15.00"),"EUR"));
        service.submit(new PaymentDtos.CreatePaymentRequest(new BigDecimal("20.00"),"GBP"));
        var page=service.search(
            PaymentStatus.PENDING,"GBP",null,null,PageRequest.of(0,1,Sort.by("amount").descending()));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().amount()).isEqualByComparingTo("20.00");
    }
}
