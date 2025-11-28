package com.example.discovery.config;

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
 * Security configuration for Eureka Server dashboard
 * Implements authentication and role-based access control
 * - Dashboard requires authentication
 * - Health endpoints are public
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
        log.info("Configuring security for Eureka Server dashboard");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                
                .requestMatchers(
                    "/actuator/env/**",
                    "/actuator/configprops/**",
                    "/actuator/beans/**",
                    "/actuator/mappings/**",
                    "/actuator/shutdown/**",
                    "/actuator/threaddump/**",
                    "/actuator/heapdump/**"
                ).hasRole("ADMIN")
                
                .requestMatchers("/actuator/metrics/**", "/actuator/prometheus").authenticated()
                
                .requestMatchers("/", "/eureka/**").authenticated()
                
                .requestMatchers("/actuator/**").authenticated()
                
                .anyRequest().authenticated()
            )
            
            .httpBasic(Customizer.withDefaults())
            
            .formLogin(form -> form
                .loginPage("/login").permitAll()
                .defaultSuccessUrl("/", true)
            )
            
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        log.info("Security configuration completed:");
        log.info("  - Public: /actuator/health, /actuator/info");
        log.info("  - Authenticated: Dashboard, metrics, registry");
        log.info("  - ADMIN role: Sensitive actuator endpoints");
        
        return http.build();
    }
}
