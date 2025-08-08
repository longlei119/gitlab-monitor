package com.gitlab.metrics.config;

import com.gitlab.metrics.exception.ExternalServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Test class for RetryConfig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RetryConfig.class})
public class RetryConfigTest {
    
    @Autowired
    private RetryTemplate retryTemplate;
    
    @Test
    public void testRetryTemplateBean() {
        assertNotNull(retryTemplate);
    }
    
    @Test
    public void testRetryOnExternalServiceException() {
        final int[] attemptCount = {0};
        
        try {
            retryTemplate.execute(context -> {
                attemptCount[0]++;
                if (attemptCount[0] < 3) {
                    throw new ExternalServiceException("TestService", "Service unavailable");
                }
                return "Success";
            });
        } catch (Exception e) {
            // Expected if all retries fail
        }
        
        assertEquals(3, attemptCount[0]);
    }
    
    @Test
    public void testSuccessfulRetry() {
        final int[] attemptCount = {0};
        
        String result = retryTemplate.execute(context -> {
            attemptCount[0]++;
            if (attemptCount[0] < 2) {
                throw new ExternalServiceException("TestService", "Service unavailable");
            }
            return "Success after retry";
        });
        
        assertEquals("Success after retry", result);
        assertEquals(2, attemptCount[0]);
    }
    
    @Test
    public void testNoRetryOnNonRetryableException() {
        final int[] attemptCount = {0};
        
        try {
            retryTemplate.execute(context -> {
                attemptCount[0]++;
                throw new IllegalArgumentException("Non-retryable exception");
            });
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        assertEquals(1, attemptCount[0]); // Should not retry
    }
}