package com.immortals.platform.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Inventory Service.
 * Manages real-time inventory tracking, reservations, and stock management.
 */
@SpringBootApplication(scanBasePackages = {
    "com.immortals.platform.inventory",
    "com.immortals.platform.common",
    "com.immortals.platform.messaging"
})
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
