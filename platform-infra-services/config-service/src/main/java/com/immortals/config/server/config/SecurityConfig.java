package com.immortals.config.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Config Server
 * Implements authentication and role-based access control
 * - All configuration endpoints require authentication
 * - Health and info endpoints are public
 * - Sensitive actuator endpoints require ADMIN role
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name:admin}")
    private String adminUsername;

    @Value("${spring.security.user.password:admin123}")
    private String adminPassword;

    /**
     * Password encoder for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * User details service with role-based users
     * Creates an admin user with ADMIN role for full access
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        log.info("Configuring user details service with admin user: {}", adminUsername);
        
        // Admin user with full access
        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(passwordEncoder.encode(adminPassword))
            .roles("ADMIN", "USER")
            .build();
        
        log.info("Admin user configured with ADMIN and USER roles");
        
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Security filter chain configuration
     * Defines authorization rules and authentication mechanisms
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security for Config Server");
        
        http
            // Disable CSRF for REST API endpoints
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - no authentication required
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                
                // Sensitive actuator endpoints - require ADMIN role
                .requestMatchers(
                    "/actuator/env/**",
                    "/actuator/configprops/**",
                    "/actuator/beans/**",
                    "/actuator/mappings/**",
                    "/actuator/shutdown/**",
                    "/actuator/threaddump/**",
                    "/actuator/heapdump/**"
                ).hasRole("ADMIN")
                
                // Metrics and Prometheus endpoints - require authentication
                .requestMatchers("/actuator/metrics/**", "/actuator/prometheus").authenticated()
                
                // Encryption/decryption endpoints - require authentication
                .requestMatchers("/encrypt/**", "/decrypt/**").authenticated()
                
                // Configuration endpoints - require authentication
                .requestMatchers("/**/*.yml", "/**/*.yaml", "/**/*.properties", "/**/*.json").authenticated()
                
                // Refresh endpoints - require authentication
                .requestMatchers("/actuator/refresh", "/actuator/busrefresh/**").authenticated()
                
                // Monitor/webhook endpoints - require authentication
                .requestMatchers("/monitor/**").authenticated()
                
                // All other actuator endpoints - require authentication
                .requestMatchers("/actuator/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Enable HTTP Basic authentication
            .httpBasic(Customizer.withDefaults());
        
        log.info("Security configuration completed:");
        log.info("  - Public: /actuator/health, /actuator/info");
        log.info("  - Authenticated: Config, encryption, refresh, metrics");
        log.info("  - ADMIN role: Sensitive actuator endpoints");
        
        return http.build();
    }
}
