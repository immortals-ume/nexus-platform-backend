package com.immortals.otpservice.config.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.jpa.properties")
public class JpaPropertiesConfig {
    private Map<String, Object> hibernate;
    private Map<String, Object> org;
    private String databasePlatform;
}