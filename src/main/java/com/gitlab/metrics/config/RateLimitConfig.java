package com.gitlab.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API限流配置
 * 配置API访问频率限制
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {
    
    @Bean
    public RateLimitInterceptor rateLimitInterceptor(RedisTemplate<String, Object> redisTemplate) {
        return new RateLimitInterceptor(redisTemplate);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor(null))
            .addPathPatterns("/api/v1/metrics/**")
            .excludePathPatterns("/api/v1/metrics/health");
    }
}