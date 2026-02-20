package com.immortals.platform.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Database configuration properties
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "datasource.db")
public class DatabaseProperties {

    private boolean enabled;
    private String defaultSchema;
    private int connectionTimeout;
    private int maxPoolSize;
    private boolean showSql;
    private String ddlAuto;
}