package com.immortals.platform.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for Kafka messaging.
 * Configures Kafka producers and consumers with JSON serialization,
 * error handling, and proper consumer group management.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableKafka
@EnableConfigurationProperties(MessagingProperties.class)
@RequiredArgsConstructor
public class KafkaAutoConfiguration {

    private final MessagingProperties messagingProperties;

    /**
     * Configure ObjectMapper for JSON serialization/deserialization
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaObjectMapper")
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * Configure Kafka producer factory with JSON serialization
     */
    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
            messagingProperties.getKafka().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
            StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
            JsonSerializer.class);
        
        // Producer reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Compression
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Batching for better throughput
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        
        log.info("Configured Kafka producer with bootstrap servers: {}", 
            messagingProperties.getKafka().getBootstrapServers());
        
        DefaultKafkaProducerFactory<String, Object> factory = 
            new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(new JsonSerializer<>(kafkaObjectMapper));
        
        return factory;
    }

    /**
     * Configure Kafka template for sending messages
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        
        // Set default topic if needed
        template.setObservationEnabled(true);
        
        log.info("Configured KafkaTemplate with observation enabled");
        return template;
    }

    /**
     * Configure Kafka consumer factory with JSON deserialization
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper kafkaObjectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
            messagingProperties.getKafka().getBootstrapServers());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
            StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
            ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, 
            JsonDeserializer.class);
        
        // Consumer group configuration
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, 
            messagingProperties.getKafka().getConsumerGroupPrefix());
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, 
            messagingProperties.getKafka().getAutoOffsetReset());
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, 
            messagingProperties.getKafka().isAutoCommit());
        
        // Session and heartbeat configuration
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 
            (int) messagingProperties.getKafka().getSessionTimeout().toMillis());
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 
            (int) messagingProperties.getKafka().getHeartbeatInterval().toMillis());
        
        // Max poll records
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 
            messagingProperties.getKafka().getMaxPollRecords());
        
        // JSON deserializer configuration
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        log.info("Configured Kafka consumer with bootstrap servers: {} and group prefix: {}", 
            messagingProperties.getKafka().getBootstrapServers(),
            messagingProperties.getKafka().getConsumerGroupPrefix());
        
        DefaultKafkaConsumerFactory<String, Object> factory = 
            new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(new ErrorHandlingDeserializer<>(
            new JsonDeserializer<>(Object.class, kafkaObjectMapper, false)));
        
        return factory;
    }

    /**
     * Configure Kafka listener container factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(messagingProperties.getKafka().getConcurrency());
        
        // Manual acknowledgment mode for better control
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // Enable observation for distributed tracing
        factory.getContainerProperties().setObservationEnabled(true);
        
        log.info("Configured Kafka listener container factory with concurrency: {}", 
            messagingProperties.getKafka().getConcurrency());
        
        return factory;
    }
}
