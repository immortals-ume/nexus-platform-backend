package com.example.discovery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Eureka Server dashboard
 * Implements basic authentication for dashboard access
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security for Eureka Server dashboard");
        
        http
            // Disable CSRF for Eureka endpoints (required for service registration)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Allow health checks without authentication
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Require authentication for Eureka dashboard and other actuator endpoints
                .requestMatchers("/", "/eureka/**", "/actuator/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Enable HTTP Basic authentication
            .httpBasic(Customizer.withDefaults());
        
        log.info("Security configuration completed - Basic authentication enabled for Eureka dashboard");
        
        return http.build();
    }
}
