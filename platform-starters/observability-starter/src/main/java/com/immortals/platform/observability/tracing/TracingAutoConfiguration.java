package com.immortals.platform.observability.tracing;

import com.immortals.platform.observability.config.ObservabilityProperties;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import io.opentelemetry.semconv.ServiceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for distributed tracing using Micrometer Tracing with OpenTelemetry.
 * Configures trace context propagation across HTTP boundaries and Zipkin exporter.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "platform.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * Configure OpenTelemetry SDK with Zipkin exporter and sampling
     */
    @Bean
    public OpenTelemetry openTelemetry(ObservabilityProperties properties) {
        log.info("Configuring OpenTelemetry with Zipkin exporter at: {}", 
                properties.getTracing().getZipkinUrl());

        // Create Zipkin span exporter
        ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder()
                .setEndpoint(properties.getTracing().getZipkinUrl() + "/api/v2/spans")
                .build();

        // Create resource with service name
        Resource resource = Resource.create(
                Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)
        );

        // Configure sampler based on sampling rate
        Sampler sampler = Sampler.traceIdRatioBased(properties.getTracing().getSamplingRate());

        // Build tracer provider with batch span processor
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(sampler)
                .addSpanProcessor(BatchSpanProcessor.builder(zipkinExporter).build())
                .build();

        // Build OpenTelemetry SDK with W3C trace context propagation
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        log.info("OpenTelemetry configured successfully with sampling rate: {}", 
                properties.getTracing().getSamplingRate());

        return openTelemetry;
    }

    /**
     * Create Micrometer Tracer from OpenTelemetry
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracer(serviceName);
        return new OtelTracer(otelTracer, null, null);
    }
}
