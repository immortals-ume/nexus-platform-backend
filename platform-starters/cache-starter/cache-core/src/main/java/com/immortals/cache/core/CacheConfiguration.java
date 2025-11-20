package com.immortals.cache.core;

import lombok.Getter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a cache instance.
 * Provides settings for TTL, eviction policies, and provider-specific options.
 * 
 * <p>Supports per-namespace configuration including:
 * <ul>
 *   <li>TTL (time-to-live) for cache entries</li>
 *   <li>Eviction policy (LRU, LFU, FIFO, TTL)</li>
 *   <li>Feature toggles (compression, encryption)</li>
 *   <li>Provider-specific settings</li>
 * </ul>
 * 
 * <p>Requirements: 8.1, 8.2, 8.3
 */
@Getter
public class CacheConfiguration {
    
    private Duration ttl;
    private EvictionPolicy evictionPolicy;
    private boolean compressionEnabled;
    private boolean encryptionEnabled;
    private boolean stampedeProtectionEnabled;
    private boolean circuitBreakerEnabled;
    private Map<String, Object> providerSpecificConfig;
    
    public CacheConfiguration() {
        this.ttl = Duration.ofHours(1);
        this.evictionPolicy = EvictionPolicy.LRU;
        this.compressionEnabled = false;
        this.encryptionEnabled = false;
        this.stampedeProtectionEnabled = false;
        this.circuitBreakerEnabled = false;
        this.providerSpecificConfig = new HashMap<>();
    }
    
    /**
     * Creates a copy of this configuration.
     * 
     * @return a new CacheConfiguration with the same settings
     */
    public CacheConfiguration copy() {
        CacheConfiguration copy = new CacheConfiguration();
        copy.ttl = this.ttl;
        copy.evictionPolicy = this.evictionPolicy;
        copy.compressionEnabled = this.compressionEnabled;
        copy.encryptionEnabled = this.encryptionEnabled;
        copy.stampedeProtectionEnabled = this.stampedeProtectionEnabled;
        copy.circuitBreakerEnabled = this.circuitBreakerEnabled;
        copy.providerSpecificConfig = new HashMap<>(this.providerSpecificConfig);
        return copy;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public void setEvictionPolicy(EvictionPolicy evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public void setProviderSpecificConfig(Map<String, Object> providerSpecificConfig) {
        this.providerSpecificConfig = providerSpecificConfig;
    }

    public void setStampedeProtectionEnabled(boolean stampedeProtectionEnabled) {
        this.stampedeProtectionEnabled = stampedeProtectionEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }
    
    @Override
    public String toString() {
        return "CacheConfiguration{" +
                "ttl=" + ttl +
                ", evictionPolicy=" + evictionPolicy +
                ", compressionEnabled=" + compressionEnabled +
                ", encryptionEnabled=" + encryptionEnabled +
                ", stampedeProtectionEnabled=" + stampedeProtectionEnabled +
                ", circuitBreakerEnabled=" + circuitBreakerEnabled +
                '}';
    }
    
    public enum EvictionPolicy {
        LRU,  // Least Recently Used
        LFU,  // Least Frequently Used
        FIFO, // First In First Out
        TTL   // Time To Live
    }
}
