package com.gitlab.metrics.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置
 * 配置Redis缓存管理器和缓存策略
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // 默认缓存30分钟
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("dashboard", config.entryTtl(Duration.ofMinutes(15))) // 看板数据缓存15分钟
            .withCacheConfiguration("metrics", config.entryTtl(Duration.ofMinutes(10))) // 指标数据缓存10分钟
            .withCacheConfiguration("realtime", config.entryTtl(Duration.ofMinutes(1))) // 实时数据缓存1分钟
            .build();
    }
    
    /**
     * 本地缓存管理器（作为备用）
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager("dashboard", "metrics", "realtime");
    }
}