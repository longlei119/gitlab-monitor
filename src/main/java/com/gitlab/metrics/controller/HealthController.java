package com.gitlab.metrics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("database_error", e.getMessage());
        }
        
        // Check Redis connectivity
        try {
            redisTemplate.opsForValue().set("health_check", "test");
            String value = (String) redisTemplate.opsForValue().get("health_check");
            if ("test".equals(value)) {
                health.put("redis", "UP");
            } else {
                health.put("redis", "DOWN");
            }
            redisTemplate.delete("health_check");
        } catch (Exception e) {
            health.put("redis", "DOWN");
            health.put("redis_error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}