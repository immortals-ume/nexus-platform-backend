package com.immortals.authapp.config.cache;


import com.immortals.platform.domain.properties.CacheProperties;
import io.lettuce.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static io.lettuce.core.ReadFrom.REPLICA_PREFERRED;

@AutoConfiguration
@Slf4j
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfiguration {

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory lettuceConnectionFactory(@Qualifier("cacheProperties") CacheProperties props) {
        log.info("Initializing LettuceConnectionFactory with host: {}, port: {}", props.getHost(), props.getPort());

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(props.getHost());
        redisConfig.setPort(props.getPort());
        if (props.getPassword() != null && !props.getPassword()
                .isBlank()) {
            redisConfig.setPassword(props.getPassword());
        }
        redisConfig.setDatabase(props.getDatabase());


        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(props.getCommandTimeout())
                .useSsl()
                .and()
                .shutdownTimeout(Duration.ZERO)
                .readFrom(REPLICA_PREFERRED)
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(props.getAutoReconnect())
                        .pingBeforeActivateConnection(props.getAutoReconnect())
                        .build())
                .build();


        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("lettuceConnectionFactory") RedisConnectionFactory factory) {
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

}
