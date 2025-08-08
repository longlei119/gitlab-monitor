package com.gitlab.metrics.service;

import com.gitlab.metrics.config.CircuitBreakerConfig;
import com.gitlab.metrics.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * Service demonstrating retry and circuit breaker patterns for external service calls
 */
@Service
public class ExternalServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);
    
    @Autowired
    private RetryTemplate retryTemplate;
    
    @Autowired
    private CircuitBreakerConfig.CircuitBreakerRegistry circuitBreakerRegistry;
    
    /**
     * Example method with retry annotation
     */
    @Retryable(
        value = {ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callExternalServiceWithRetry(String serviceName, String request) {
        logger.debug("Calling external service: {} with request: {}", serviceName, request);
        
        // Simulate external service call that might fail
        if (Math.random() < 0.3) { // 30% chance of failure
            throw new ExternalServiceException(serviceName, "Service temporarily unavailable");
        }
        
        return "Response from " + serviceName;
    }
    
    /**
     * Example method using RetryTemplate programmatically
     */
    public String callExternalServiceWithRetryTemplate(String serviceName, String request) {
        return retryTemplate.execute(context -> {
            logger.debug("Retry attempt {} for service: {}", context.getRetryCount() + 1, serviceName);
            
            // Simulate external service call
            if (Math.random() < 0.4) { // 40% chance of failure
                throw new ExternalServiceException(serviceName, "Service call failed");
            }
            
            return "Response from " + serviceName + " via RetryTemplate";
        });
    }
    
    /**
     * Example method using circuit breaker
     */
    public String callExternalServiceWithCircuitBreaker(String serviceName, String request) {
        CircuitBreakerConfig.CircuitBreaker circuitBreaker = circuitBreakerRegistry.getCircuitBreaker(serviceName);
        
        try {
            return circuitBreaker.execute(() -> {
                logger.debug("Calling service: {} through circuit breaker", serviceName);
                
                // Simulate external service call
                if (Math.random() < 0.2) { // 20% chance of failure
                    throw new ExternalServiceException(serviceName, "Service call failed");
                }
                
                return "Response from " + serviceName + " via Circuit Breaker";
            });
        } catch (Exception e) {
            if (e instanceof ExternalServiceException) {
                throw (ExternalServiceException) e;
            }
            throw new ExternalServiceException(serviceName, "Circuit breaker error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Example method combining retry and circuit breaker
     */
    public String callExternalServiceWithBothPatterns(String serviceName, String request) {
        CircuitBreakerConfig.CircuitBreaker circuitBreaker = circuitBreakerRegistry.getCircuitBreaker(serviceName);
        
        return retryTemplate.execute(context -> {
            logger.debug("Combined pattern - Retry attempt {} for service: {}", context.getRetryCount() + 1, serviceName);
            
            try {
                return circuitBreaker.execute(() -> {
                    // Simulate external service call
                    if (Math.random() < 0.3) { // 30% chance of failure
                        throw new ExternalServiceException(serviceName, "Service call failed");
                    }
                    
                    return "Response from " + serviceName + " via Combined Patterns";
                });
            } catch (Exception e) {
                if (e instanceof ExternalServiceException) {
                    throw (ExternalServiceException) e;
                }
                throw new ExternalServiceException(serviceName, "Combined pattern error: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get circuit breaker status for monitoring
     */
    public String getCircuitBreakerStatus(String serviceName) {
        CircuitBreakerConfig.CircuitBreaker circuitBreaker = circuitBreakerRegistry.getCircuitBreaker(serviceName);
        return String.format("Service: %s, State: %s, Failures: %d", 
            serviceName, circuitBreaker.getState(), circuitBreaker.getFailureCount());
    }
}