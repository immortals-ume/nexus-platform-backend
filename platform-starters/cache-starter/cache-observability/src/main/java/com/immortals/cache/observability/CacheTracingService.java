package com.immortals.cache.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenTelemetry tracing support for cache operations.
 * Instruments cache operations with distributed tracing spans.
 * 
 * @since 2.0.0
 */
@Component
@ConditionalOnClass(OpenTelemetry.class)
@Slf4j
public class CacheTracingService {
    
    private final Tracer tracer;
    private final Map<Object, SpanContext> activeSpans;
    
    public CacheTracingService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("cache-service", "2.0.0");
        this.activeSpans = new ConcurrentHashMap<>();
        log.info("OpenTelemetry tracing enabled for cache operations");
    }
    
    /**
     * Starts a new span for a cache operation.
     * 
     * @param operationName the operation name (e.g., "cache.get", "cache.put")
     * @param cacheName the cache name
     * @param namespace the namespace
     * @return span context object
     */
    public Object startSpan(String operationName, String cacheName, String namespace) {
        Span span = tracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("cache.name", cacheName)
            .setAttribute("cache.namespace", namespace)
            .setAttribute("cache.operation", operationName)
            .startSpan();
        
        Scope scope = span.makeCurrent();
        SpanContext context = new SpanContext(span, scope);
        activeSpans.put(context, context);
        
        return context;
    }
    
    /**
     * Adds an attribute to an active span.
     * 
     * @param spanContext the span context
     * @param key the attribute key
     * @param value the attribute value
     */
    public void addAttribute(Object spanContext, String key, String value) {
        if (spanContext instanceof SpanContext) {
            SpanContext context = (SpanContext) spanContext;
            context.span.setAttribute(key, value);
        }
    }
    
    /**
     * Adds a boolean attribute to an active span.
     * 
     * @param spanContext the span context
     * @param key the attribute key
     * @param value the attribute value
     */
    public void addAttribute(Object spanContext, String key, boolean value) {
        if (spanContext instanceof SpanContext) {
            SpanContext context = (SpanContext) spanContext;
            context.span.setAttribute(key, value);
        }
    }
    
    /**
     * Adds a long attribute to an active span.
     * 
     * @param spanContext the span context
     * @param key the attribute key
     * @param value the attribute value
     */
    public void addAttribute(Object spanContext, String key, long value) {
        if (spanContext instanceof SpanContext) {
            SpanContext context = (SpanContext) spanContext;
            context.span.setAttribute(key, value);
        }
    }
    
    /**
     * Records an error on the span.
     * 
     * @param spanContext the span context
     * @param error the error that occurred
     */
    public void recordError(Object spanContext, Throwable error) {
        if (spanContext instanceof SpanContext) {
            SpanContext context = (SpanContext) spanContext;
            context.span.recordException(error);
            context.span.setStatus(StatusCode.ERROR, error.getMessage());
        }
    }
    
    /**
     * Ends the span and closes the scope.
     * 
     * @param spanContext the span context
     */
    public void endSpan(Object spanContext) {
        if (spanContext instanceof SpanContext) {
            SpanContext context = (SpanContext) spanContext;
            try {
                context.scope.close();
                context.span.end();
            } finally {
                activeSpans.remove(spanContext);
            }
        }
    }
    
    /**
     * Internal class to hold span and scope together.
     */
    private static class SpanContext {
        final Span span;
        final Scope scope;
        
        SpanContext(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }
    }
}
