package com.gitlab.metrics.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置
 * 配置Redis缓存管理器和缓存策略，优化缓存性能
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Redis缓存管理器 - 优化版本
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // 默认缓存30分钟
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues()
            .prefixCacheNameWith("gitlab-metrics:");
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            // 看板数据 - 中等缓存时间，因为数据相对稳定
            .withCacheConfiguration("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            // 指标数据 - 较短缓存时间，需要相对实时
            .withCacheConfiguration("metrics", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            // 实时数据 - 很短缓存时间
            .withCacheConfiguration("realtime", defaultConfig.entryTtl(Duration.ofMinutes(1)))
            // 统计数据 - 较长缓存时间，计算成本高
            .withCacheConfiguration("statistics", defaultConfig.entryTtl(Duration.ofMinutes(60)))
            // 质量分析 - 中等缓存时间
            .withCacheConfiguration("quality", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            // 开发者数据 - 较长缓存时间
            .withCacheConfiguration("developer", defaultConfig.entryTtl(Duration.ofMinutes(45)))
            // 项目数据 - 长缓存时间，变化不频繁
            .withCacheConfiguration("project", defaultConfig.entryTtl(Duration.ofHours(2)))
            // 趋势数据 - 长缓存时间，计算复杂
            .withCacheConfiguration("trends", defaultConfig.entryTtl(Duration.ofHours(1)))
            // 报告数据 - 很长缓存时间
            .withCacheConfiguration("reports", defaultConfig.entryTtl(Duration.ofHours(4)))
            .transactionAware()
            .build();
    }
    
    /**
     * 本地缓存管理器（作为备用和快速访问）
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager(
            "dashboard", "metrics", "realtime", "statistics", 
            "quality", "developer", "project", "trends", "reports"
        );
    }
    
    /**
     * 多级缓存管理器（L1本地缓存 + L2 Redis缓存）
     */
    @Bean("multiLevelCacheManager")
    public CacheManager multiLevelCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 这里可以实现多级缓存，先查本地缓存，再查Redis缓存
        // 为简化实现，暂时返回Redis缓存管理器
        return cacheManager(redisConnectionFactory);
    }
}