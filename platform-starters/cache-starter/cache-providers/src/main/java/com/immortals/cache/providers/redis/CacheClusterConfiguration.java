package com.immortals.cache.providers.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(
    name = "immortals.cache.redis.cluster.nodes",
    matchIfMissing = false
)
public class CacheClusterConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheClusterConfiguration.class);


    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory redisClusterConnectionFactory(final RedisProperties props) {
        log.info("Initializing Redis Cluster connection with nodes: {}", props.getCluster().getNodes());

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(props.getCluster().getNodes());

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

        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisClusterTemplate(RedisConnectionFactory redisClusterConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisClusterConnectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        log.info("Redis Cluster Template initialized");
        return template;
    }
}
