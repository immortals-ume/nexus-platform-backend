package com.immortals.notification.service.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer interceptor to extract correlation ID from message headers
 * and add it to MDC for distributed tracing
 */
@Slf4j
public class KafkaCorrelationIdInterceptor implements ConsumerInterceptor<String, Object> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        records.forEach(record -> {
            String correlationId = extractCorrelationId(record.headers());
            
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new correlation ID for Kafka message: {}", correlationId);
            } else {
                log.debug("Using correlation ID from Kafka message header: {}", correlationId);
            }
            
            // Add to MDC
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        });
        
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Clean up MDC after commit
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }

    @Override
    public void close() {
        // Cleanup if needed
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Configuration if needed
    }

    /**
     * Extract correlation ID from Kafka message headers
     */
    private String extractCorrelationId(org.apache.kafka.common.header.Headers headers) {
        if (headers == null) {
            return null;
        }

        Header correlationIdHeader = headers.lastHeader(CORRELATION_ID_HEADER);
        if (correlationIdHeader == null) {
            return null;
        }

        byte[] value = correlationIdHeader.value();
        if (value == null || value.length == 0) {
            return null;
        }

        return new String(value, StandardCharsets.UTF_8);
    }
}
