package com.immortals.platform.inventory.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Kafka topics used by Inventory Service.
 */
@Configuration
@Getter
public class KafkaTopicConfig {

    @Value("${kafka.topics.inventory-reserved:inventory.reserved}")
    private String inventoryReservedTopic;

    @Value("${kafka.topics.inventory-released:inventory.released}")
    private String inventoryReleasedTopic;

    @Value("${kafka.topics.low-inventory:inventory.low-stock}")
    private String lowInventoryTopic;

    @Value("${kafka.topics.inventory-updated:inventory.updated}")
    private String inventoryUpdatedTopic;
}
