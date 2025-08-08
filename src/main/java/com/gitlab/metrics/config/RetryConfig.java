package com.gitlab.metrics.config;

import com.gitlab.metrics.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for retry mechanism
 */
@Configuration
@EnableRetry
public class RetryConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryConfig.class);
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // 1 second
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L); // 10 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure retry policy with specific exceptions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ExternalServiceException.class, true);
        retryableExceptions.put(RuntimeException.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Add retry listener for logging
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                logger.debug("Starting retry operation: {}", context.getAttribute("context.name"));
                return true;
            }
            
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                if (throwable != null) {
                    logger.warn("Retry operation failed after {} attempts: {}", 
                        context.getRetryCount(), throwable.getMessage());
                } else {
                    logger.debug("Retry operation succeeded after {} attempts", context.getRetryCount());
                }
            }
            
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                logger.debug("Retry attempt {} failed: {}", context.getRetryCount(), throwable.getMessage());
            }
        });
        
        return retryTemplate;
    }
}