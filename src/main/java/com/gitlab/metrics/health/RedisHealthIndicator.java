package com.gitlab.metrics.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Custom health indicator for Redis connectivity and performance
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            return checkRedisHealth();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("redis", "Connection failed")
                .build();
        }
    }
    
    private Health checkRedisHealth() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Test basic connectivity with ping
        String pong = redisTemplate.getConnectionFactory().getConnection().ping();
        if (!"PONG".equals(pong)) {
            return Health.down()
                .withDetail("redis", "Ping failed")
                .withDetail("response", pong)
                .build();
        }
        
        // Test read/write operations
        String testKey = "health_check_" + System.currentTimeMillis();
        String testValue = "test_value";
        
        try {
            // Write test
            redisTemplate.opsForValue().set(testKey, testValue);
            
            // Read test
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            
            // Cleanup
            redisTemplate.delete(testKey);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (testValue.equals(retrievedValue)) {
                Health.Builder builder = Health.up()
                    .withDetail("redis", "UP")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("operations", "ping, set, get, delete");
                
                // Get Redis info
                try {
                    Properties info = redisTemplate.getConnectionFactory().getConnection().info();
                    if (info != null) {
                        builder.withDetail("version", info.getProperty("redis_version"))
                               .withDetail("mode", info.getProperty("redis_mode"))
                               .withDetail("connected_clients", info.getProperty("connected_clients"))
                               .withDetail("used_memory_human", info.getProperty("used_memory_human"));
                    }
                } catch (Exception e) {
                    // Info command failed, but basic operations work
                    builder.withDetail("info_warning", "Could not retrieve Redis info");
                }
                
                // Add performance warning if response time is high
                if (responseTime > 500) {
                    builder.withDetail("warning", "High response time detected");
                }
                
                return builder.build();
            } else {
                return Health.down()
                    .withDetail("redis", "Read/write test failed")
                    .withDetail("expected", testValue)
                    .withDetail("actual", retrievedValue)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "Read/write operations failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}