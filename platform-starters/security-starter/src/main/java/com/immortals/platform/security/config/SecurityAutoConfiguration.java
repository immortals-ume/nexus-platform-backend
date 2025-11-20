package com.immortals.platform.security.config;

import com.immortals.platform.security.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPublicKey;

/**
 * Auto-configuration for security-starter.
 * Configures OAuth2 resource server with JWT validation.
 */
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableCaching
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(name = "platform.security.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    @Bean
    public JwtUtils jwtUtils(SecurityProperties securityProperties) throws Exception {
        log.info("Initializing JWT Utils");
        
        String publicKeyPem = securityProperties.getJwt().getPublicKey();
        String issuer = securityProperties.getJwt().getIssuer();
        boolean cacheEnabled = securityProperties.getJwt().getCacheEnabled();
        
        if (publicKeyPem == null || publicKeyPem.isEmpty()) {
            throw new IllegalArgumentException("JWT public key must be configured");
        }
        
        RSAPublicKey publicKey = JwtUtils.loadPublicKeyFromPem(publicKeyPem);
        return new JwtUtils(publicKey, issuer, cacheEnabled);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        log.info("Configuring CORS");
        
        SecurityProperties.Cors corsProps = securityProperties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
        configuration.setAllowedMethods(corsProps.getAllowedMethods());
        configuration.setAllowedHeaders(corsProps.getAllowedHeaders());
        configuration.setAllowCredentials(corsProps.getAllowCredentials());
        configuration.setMaxAge(corsProps.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.info("Configuring JWT Authentication Converter");
        
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, 
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        log.info("Configuring Security Filter Chain");
        
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }
}
