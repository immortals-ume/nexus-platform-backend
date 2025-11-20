package com.example.config_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.bus.enabled", havingValue = "true")
public class BusConfig {

    @Bean
    public BusProperties busProperties() {
        BusProperties properties = new BusProperties();
        log.info("Spring Cloud Bus enabled for configuration refresh");
        return properties;
    }
}
