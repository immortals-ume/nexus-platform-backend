package com.immortals.config.server;

import com.immortals.config.server.config.ConfigServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server Application.
 * 
 * <p>This application provides centralized configuration management for microservices
 * with the following features:</p>
 * <ul>
 *   <li>Git-backed configuration storage</li>
 *   <li>Symmetric and asymmetric encryption/decryption</li>
 *   <li>Configuration refresh via Spring Cloud Bus (Kafka)</li>
 *   <li>Audit logging for configuration access</li>
 *   <li>Metrics and observability</li>
 *   <li>Security with authentication and authorization</li>
 * </ul>
 * 
 * <p><b>Key Endpoints:</b></p>
 * <ul>
 *   <li>{@code /{application}/{profile}} - Get configuration for application and profile</li>
 *   <li>{@code /encrypt} - Encrypt sensitive values</li>
 *   <li>{@code /decrypt} - Decrypt encrypted values</li>
 *   <li>{@code /actuator/refresh} - Trigger configuration refresh</li>
 *   <li>{@code /actuator/health} - Health check endpoint</li>
 * </ul>
 * 
 * @author Platform Team
 * @version 1.0.0
 * @since 1.0.0
 * @see ConfigServerProperties
 */
@SpringBootApplication
@EnableConfigServer
@EnableConfigurationProperties(ConfigServerProperties.class)
public class ConfigServerApplication {

	/**
	 * Main entry point for the Config Server application.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
