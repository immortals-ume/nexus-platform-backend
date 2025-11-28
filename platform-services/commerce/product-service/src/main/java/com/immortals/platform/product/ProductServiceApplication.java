package com.immortals.platform.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Product Service.
 * Enables JPA auditing for automatic timestamp and user tracking.
 * Enables Eureka discovery client for service registration.
 */
@SpringBootApplication(scanBasePackages = {
    "com.immortals.platform.product",
    "com.immortals.platform.common",
    "com.immortals.platform.domain"
})
@EnableJpaAuditing
@EnableDiscoveryClient
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
