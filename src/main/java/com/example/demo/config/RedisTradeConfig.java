package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;
import java.time.Duration;

@Configuration
public class RedisTradeConfig {

    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(uri.getHost());
        config.setPort(uri.getPort() > 0 ? uri.getPort() : 6379);
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":", 2);
            if (userInfo.length == 2 && !userInfo[1].isEmpty()) {
                config.setPassword(userInfo[1]);
            }
        }
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setEagerInitialization(false);
        return factory;
    }
    
    /**
     * StringRedisTemplate bean for Redis operations
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
