package com.immortals.platform.domain.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cookie")
public class CookieProperties {
    private Boolean secure;
    private Integer maxAge;
}