package com.immortals.platform.product.config;

import lombok.Getter;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration for Kafka topics used by Product Service.
 * Centralizes topic names for event publishing and creates topic beans.
 */
@Configuration
@Getter
public class KafkaTopicConfig {

    @Value("${kafka.topics.product-created:product-created-topic}")
    private String productCreatedTopic;

    @Value("${kafka.topics.product-updated:product-updated-topic}")
    private String productUpdatedTopic;

    @Value("${kafka.topics.product-deleted:product-deleted-topic}")
    private String productDeletedTopic;

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder
                .name(productCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder
                .name(productUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder
                .name(productDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder
                .name("order-created-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder
                .name("order-confirmed-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
