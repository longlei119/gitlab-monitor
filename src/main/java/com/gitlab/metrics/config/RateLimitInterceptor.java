package com.gitlab.metrics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * API限流拦截器
 * 基于Redis实现的API访问频率限制
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 限流配置
    private static final int DEFAULT_LIMIT = 100; // 默认每分钟100次请求
    private static final int DASHBOARD_LIMIT = 20; // 看板API每分钟20次请求
    private static final int REALTIME_LIMIT = 60; // 实时API每分钟60次请求
    private static final long WINDOW_SIZE = 60; // 时间窗口60秒
    
    public RateLimitInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (redisTemplate == null) {
            return true; // 如果Redis不可用，跳过限流
        }
        
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        
        // 根据不同的API端点设置不同的限流策略
        int limit = getLimit(requestUri);
        String key = "rate_limit:" + clientIp + ":" + requestUri;
        
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount == 1) {
                // 第一次访问，设置过期时间
                redisTemplate.expire(key, WINDOW_SIZE, TimeUnit.SECONDS);
            }
            
            if (currentCount > limit) {
                logger.warn("API限流触发: IP={}, URI={}, 当前请求数={}, 限制={}", 
                           clientIp, requestUri, currentCount, limit);
                
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"API访问频率超过限制\"}");
                return false;
            }
            
            // 在响应头中添加限流信息
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + WINDOW_SIZE * 1000));
            
        } catch (Exception e) {
            logger.error("限流检查失败: {}", e.getMessage(), e);
            // 如果限流检查失败，允许请求通过
            return true;
        }
        
        return true;
    }
    
    /**
     * 根据API端点获取限流配置
     */
    private int getLimit(String requestUri) {
        if (requestUri.contains("/dashboard")) {
            return DASHBOARD_LIMIT;
        } else if (requestUri.contains("/realtime")) {
            return REALTIME_LIMIT;
        } else {
            return DEFAULT_LIMIT;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}