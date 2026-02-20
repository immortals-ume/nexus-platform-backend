package com.immortals.config.server.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "config-server")
@Validated
public class ConfigServerProperties {
    
    @Valid
    private Encryption encryption = new Encryption();
    
    @Valid
    @NotNull(message = "Refresh configuration is required")
    private Refresh refresh = new Refresh();

    @Getter
    @Setter
    @Validated
    public static class Encryption {
        private String key;

        @Valid
        private ConfigServerProperties.Encryption.KeyStore keyStore = new KeyStore();

        @Getter
        @Setter
        @Validated
        public static class KeyStore {
            private String location;
            private String password;
            private String alias;
            private String secret;
        }
    }

    @Getter
    @Setter
    @Validated
    public static class Refresh {
        private boolean enabled;

        @Min(value = 1, message = "Refresh rate must be at least 1 second")
        private int rate;
    }
}
