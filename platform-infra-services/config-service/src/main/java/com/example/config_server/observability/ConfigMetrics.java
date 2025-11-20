package com.example.config_server.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ConfigMetrics {

    private final Counter configRequestCounter;
    private final Counter configRequestSuccessCounter;
    private final Counter configRequestFailureCounter;
    private final Counter encryptionRequestCounter;
    private final Counter decryptionRequestCounter;
    private final Counter refreshEventCounter;
    private final Timer configRequestTimer;

    public ConfigMetrics(MeterRegistry meterRegistry) {
        // Configuration request metrics
        this.configRequestCounter = Counter.builder("config.requests.total")
            .description("Total number of configuration requests")
            .register(meterRegistry);
        
        this.configRequestSuccessCounter = Counter.builder("config.requests.success")
            .description("Number of successful configuration requests")
            .register(meterRegistry);
        
        this.configRequestFailureCounter = Counter.builder("config.requests.failure")
            .description("Number of failed configuration requests")
            .register(meterRegistry);
        
        // Encryption/Decryption metrics
        this.encryptionRequestCounter = Counter.builder("config.encryption.requests")
            .description("Number of encryption requests")
            .register(meterRegistry);
        
        this.decryptionRequestCounter = Counter.builder("config.decryption.requests")
            .description("Number of decryption requests")
            .register(meterRegistry);
        
        // Refresh event metrics
        this.refreshEventCounter = Counter.builder("config.refresh.events")
            .description("Number of configuration refresh events")
            .register(meterRegistry);
        
        // Timing metrics
        this.configRequestTimer = Timer.builder("config.requests.duration")
            .description("Duration of configuration requests")
            .register(meterRegistry);
        
        log.info("Config server metrics initialized");
    }

    public void recordConfigRequest() {
        configRequestCounter.increment();
    }

    public void recordConfigRequestSuccess() {
        configRequestSuccessCounter.increment();
    }

    public void recordConfigRequestFailure() {
        configRequestFailureCounter.increment();
    }

    public void recordEncryptionRequest() {
        encryptionRequestCounter.increment();
    }

    public void recordDecryptionRequest() {
        decryptionRequestCounter.increment();
    }

    public void recordRefreshEvent() {
        refreshEventCounter.increment();
    }

    public void recordConfigRequestDuration(long durationMs) {
        configRequestTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(configRequestTimer);
    }
}
