package com.immortals.platform.product.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Kafka topics used by Product Service.
 * Centralizes topic names for event publishing.
 */
@Configuration
@Getter
public class KafkaTopicConfig {

    @Value("${kafka.topics.product-created:product.created}")
    private String productCreatedTopic;

    @Value("${kafka.topics.product-updated:product.updated}")
    private String productUpdatedTopic;

    @Value("${kafka.topics.product-deleted:product.deleted}")
    private String productDeletedTopic;
}
