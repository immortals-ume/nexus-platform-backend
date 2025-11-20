package com.immortals.cache.providers.redis;


import com.immortals.cache.core.CacheService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(CacheProperties.class)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "immortals.cache.redis.sentinel.master",
    matchIfMissing = false
)
public class CacheSentinelConfiguration {
    private final MeterRegistry meterRegistry;

    @Bean
    public RedisProperties cacheProperties() {
        return new RedisProperties();
    }


    @Bean
    public LettuceConnectionFactory redisSentinelConnectionFactory(final RedisProperties props) {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(props.getSentinel()
                        .getMaster());
        props.getSentinel()
                .getNodes()
                .forEach(node -> {
                    String[] parts = node.split(":");
                    sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
                });

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(props.getCommandTimeout())
                .shutdownTimeout(Duration.ZERO)
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .useSsl()
                .and()
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(props.getAutoReconnect())
                        .pingBeforeActivateConnection(props.getPingBeforeActivateConnection())
                        .build())
                .build();

        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("redisSentinelConnectionFactory") RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate initialized with custom Jackson2JsonRedisSerializer");
        return template;
    }

    @Bean
    public CacheService<Object, Object> cacheServiceImplementation(
            RedisTemplate<String, Object> redisTemplate,
            RedisProperties redisProperties) {
        log.info("Initializing RedisCacheService for Sentinel configuration");
        return new RedisCacheService<>(redisTemplate, meterRegistry, redisProperties);
    }
}
