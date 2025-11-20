package com.example.config_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "config-server")
public class ConfigServerProperties {
    
    private Git git = new Git();
    private Encryption encryption = new Encryption();
    private Refresh refresh = new Refresh();
    
    @Data
    public static class Git {
        private String uri;
        private String username;
        private String password;
        private String defaultLabel = "main";
        private String searchPaths = "{application}";
        private boolean cloneOnStart = true;
        private int timeout = 10;
    }
    
    @Data
    public static class Encryption {
        private String key;
        private KeyStore keyStore = new KeyStore();
        
        @Data
        public static class KeyStore {
            private String location;
            private String password;
            private String alias;
            private String secret;
        }
    }
    
    @Data
    public static class Refresh {
        private boolean enabled = true;
        private int rate = 60;
    }
}
