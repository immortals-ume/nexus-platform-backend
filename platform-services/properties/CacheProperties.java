package com.immortals.platform.domain.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "cache.redis")
public class CacheProperties {

    private String host;
    private Integer port;
    private String password;
    private Integer database;
    private Duration commandTimeout;
    private Boolean useSsl;
    private Boolean autoReconnect;
    private Integer poolMaxTotal;
    private Integer poolMaxIdle;
    private Integer poolMinIdle;
    private Duration poolMaxWait;
    private Boolean enabled;
    private Boolean pingBeforeActivateConnection;
}
