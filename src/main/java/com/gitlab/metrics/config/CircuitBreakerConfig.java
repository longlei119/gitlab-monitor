package com.gitlab.metrics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple circuit breaker implementation
 */
@Configuration
public class CircuitBreakerConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return new CircuitBreakerRegistry();
    }
    
    public static class CircuitBreakerRegistry {
        private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
        
        public CircuitBreaker getCircuitBreaker(String name) {
            return circuitBreakers.computeIfAbsent(name, k -> new CircuitBreaker(name));
        }
    }
    
    public static class CircuitBreaker {
        private final String name;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private volatile State state = State.CLOSED;
        
        private static final int FAILURE_THRESHOLD = 5;
        private static final long TIMEOUT = 60000; // 1 minute
        
        public CircuitBreaker(String name) {
            this.name = name;
        }
        
        public <T> T execute(CircuitBreakerCallback<T> callback) throws Exception {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime.get() > TIMEOUT) {
                    state = State.HALF_OPEN;
                    logger.info("Circuit breaker {} moved to HALF_OPEN state", name);
                } else {
                    throw new RuntimeException("Circuit breaker is OPEN for service: " + name);
                }
            }
            
            try {
                T result = callback.call();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }
        
        private void onSuccess() {
            failureCount.set(0);
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                logger.info("Circuit breaker {} moved to CLOSED state", name);
            }
        }
        
        private void onFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (failures >= FAILURE_THRESHOLD) {
                state = State.OPEN;
                logger.warn("Circuit breaker {} moved to OPEN state after {} failures", name, failures);
            }
        }
        
        public State getState() {
            return state;
        }
        
        public int getFailureCount() {
            return failureCount.get();
        }
    }
    
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }
    
    @FunctionalInterface
    public interface CircuitBreakerCallback<T> {
        T call() throws Exception;
    }
}