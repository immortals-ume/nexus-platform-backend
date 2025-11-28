package com.immortals.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for API Gateway
 * Implements authentication and role-based access control
 * - Actuator endpoints require authentication
 * - Health and info endpoints are public
 * - Sensitive actuator endpoints require ADMIN role
 * - Proxied requests pass through without additional authentication (handled by downstream services)
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
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
     * Reactive user details service with role-based users
     * Creates an admin user with ADMIN role for full access
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        log.info("Configuring reactive user details service with admin user: {}", adminUsername);
        
        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(passwordEncoder.encode(adminPassword))
            .roles("ADMIN", "USER")
            .build();
        
        log.info("Admin user configured with ADMIN and USER roles");
        
        return new MapReactiveUserDetailsService(admin);
    }

    /**
     * Security web filter chain configuration
     * Defines authorization rules and authentication mechanisms for reactive gateway
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security for API Gateway");
        
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            .authorizeExchange(authorize -> authorize
                .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                
                .pathMatchers(
                    "/actuator/env/**",
                    "/actuator/configprops/**",
                    "/actuator/beans/**",
                    "/actuator/mappings/**",
                    "/actuator/shutdown/**",
                    "/actuator/threaddump/**",
                    "/actuator/heapdump/**"
                ).hasRole("ADMIN")
                
                .pathMatchers("/actuator/metrics/**", "/actuator/prometheus").authenticated()
                
                .pathMatchers("/actuator/gateway/**").authenticated()
                
                .pathMatchers("/actuator/**").authenticated()
                
                .pathMatchers("/fallback/**").permitAll()
                
                .anyExchange().permitAll()
            )
            
            .httpBasic(Customizer.withDefaults());
        
        log.info("Security configuration completed:");
        log.info("  - Public: /actuator/health, /actuator/info, /fallback/**, proxied requests");
        log.info("  - Authenticated: Gateway actuator, metrics");
        log.info("  - ADMIN role: Sensitive actuator endpoints");
        
        return http.build();
    }
}
