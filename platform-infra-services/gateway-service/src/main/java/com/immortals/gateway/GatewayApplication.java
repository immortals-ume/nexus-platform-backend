package com.immortals.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Service - Single entry point for all microservices
 * 
 * Features:
 * - Service discovery integration with Eureka
 * - Load balancing across service instances
 * - Rate limiting per IP address
 * - Circuit breaker for downstream services
 * - CORS configuration for web clients
 * - Request/response logging
 * - Distributed tracing
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}