package dev.portfolio.payments.payment.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorConfiguration {

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String,String> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(record.topic() + "-dlt", record.partition())),
                new FixedBackOff(1000L, 3L));
    }

}
