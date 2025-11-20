package com.immortals.platform.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Service for creating and managing custom business metrics.
 * Provides convenient methods for counters, timers, and gauges.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomMetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * Increment a counter metric
     *
     * @param metricName Name of the metric
     * @param tags       Optional tags as key-value pairs
     */
    public void incrementCounter(String metricName, String... tags) {
        Counter counter = Counter.builder(metricName)
                .tags(tags)
                .register(meterRegistry);
        counter.increment();
    }

    /**
     * Increment a counter by a specific amount
     *
     * @param metricName Name of the metric
     * @param amount     Amount to increment
     * @param tags       Optional tags as key-value pairs
     */
    public void incrementCounter(String metricName, double amount, String... tags) {
        Counter counter = Counter.builder(metricName)
                .tags(tags)
                .register(meterRegistry);
        counter.increment(amount);
    }

    /**
     * Record a timer metric
     *
     * @param metricName Name of the metric
     * @param duration   Duration to record
     * @param tags       Optional tags as key-value pairs
     */
    public void recordTimer(String metricName, Duration duration, String... tags) {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .register(meterRegistry);
        timer.record(duration);
    }

    /**
     * Time a callable operation and record the duration
     *
     * @param metricName Name of the metric
     * @param callable   Operation to time
     * @param tags       Optional tags as key-value pairs
     * @param <T>        Return type of the callable
     * @return Result of the callable
     * @throws Exception if the callable throws an exception
     */
    public <T> T timeCallable(String metricName, Callable<T> callable, String... tags) throws Exception {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .register(meterRegistry);
        return timer.recordCallable(callable);
    }

    /**
     * Time a runnable operation and record the duration
     *
     * @param metricName Name of the metric
     * @param runnable   Operation to time
     * @param tags       Optional tags as key-value pairs
     */
    public void timeRunnable(String metricName, Runnable runnable, String... tags) {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .register(meterRegistry);
        timer.record(runnable);
    }

    /**
     * Register a gauge metric
     *
     * @param metricName Name of the metric
     * @param obj        Object to observe
     * @param valueFunction Function to extract the value
     * @param tags       Optional tags as key-value pairs
     * @param <T>        Type of the object
     */
    public <T> void registerGauge(String metricName, T obj, 
                                   java.util.function.ToDoubleFunction<T> valueFunction, 
                                   String... tags) {
        meterRegistry.gauge(metricName, 
                io.micrometer.core.instrument.Tags.of(tags), 
                obj, 
                valueFunction);
    }
}
