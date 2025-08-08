package com.gitlab.metrics.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for CircuitBreakerConfig
 */
public class CircuitBreakerConfigTest {
    
    private CircuitBreakerConfig.CircuitBreakerRegistry registry;
    private CircuitBreakerConfig.CircuitBreaker circuitBreaker;
    
    @Before
    public void setUp() {
        registry = new CircuitBreakerConfig.CircuitBreakerRegistry();
        circuitBreaker = registry.getCircuitBreaker("testService");
    }
    
    @Test
    public void testCircuitBreakerInitialState() {
        assertEquals(CircuitBreakerConfig.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }
    
    @Test
    public void testSuccessfulExecution() throws Exception {
        String result = circuitBreaker.execute(() -> "Success");
        
        assertEquals("Success", result);
        assertEquals(CircuitBreakerConfig.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }
    
    @Test
    public void testFailureIncrementsCount() {
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("Test failure");
            });
        } catch (Exception e) {
            // Expected
        }
        
        assertEquals(1, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreakerConfig.State.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    public void testCircuitBreakerOpensAfterThreshold() {
        // Cause 5 failures to reach threshold
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure " + i);
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(5, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreakerConfig.State.OPEN, circuitBreaker.getState());
    }
    
    @Test
    public void testCircuitBreakerRejectsWhenOpen() {
        // Open the circuit breaker
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        // Now it should reject calls
        try {
            circuitBreaker.execute(() -> "Should not execute");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Circuit breaker is OPEN"));
        } catch (Exception e) {
            fail("Expected RuntimeException, got: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testSuccessResetsFailureCount() throws Exception {
        // Cause some failures
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(3, circuitBreaker.getFailureCount());
        
        // Now succeed
        String result = circuitBreaker.execute(() -> "Success");
        
        assertEquals("Success", result);
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreakerConfig.State.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    public void testRegistryReturnsSameInstance() {
        CircuitBreakerConfig.CircuitBreaker cb1 = registry.getCircuitBreaker("service1");
        CircuitBreakerConfig.CircuitBreaker cb2 = registry.getCircuitBreaker("service1");
        
        assertSame(cb1, cb2);
    }
    
    @Test
    public void testRegistryReturnsDifferentInstancesForDifferentServices() {
        CircuitBreakerConfig.CircuitBreaker cb1 = registry.getCircuitBreaker("service1");
        CircuitBreakerConfig.CircuitBreaker cb2 = registry.getCircuitBreaker("service2");
        
        assertNotSame(cb1, cb2);
    }
}