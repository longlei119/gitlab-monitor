package com.gitlab.metrics.controller;

import com.gitlab.metrics.config.CircuitBreakerConfig;
import com.gitlab.metrics.monitoring.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthEndpoint;
import org.springframework.boot.actuator.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for MonitoringController
 */
@RunWith(MockitoJUnitRunner.class)
public class MonitoringControllerTest {
    
    @InjectMocks
    private MonitoringController monitoringController;
    
    @Mock
    private BusinessMetrics businessMetrics;
    
    @Mock
    private CircuitBreakerConfig.CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Mock
    private HealthEndpoint healthEndpoint;
    
    private MeterRegistry meterRegistry;
    
    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        
        // Use reflection to set the meterRegistry field
        try {
            java.lang.reflect.Field field = MonitoringController.class.getDeclaredField("meterRegistry");
            field.setAccessible(true);
            field.set(monitoringController, meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testGetMetricsSummary() {
        // Given - add some test metrics
        meterRegistry.counter("webhook.received.total").increment(10);
        meterRegistry.counter("webhook.processed.total").increment(8);
        meterRegistry.counter("webhook.failed.total").increment(2);
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getMetricsSummary();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertTrue(body.containsKey("webhooks"));
        assertTrue(body.containsKey("commits"));
        assertTrue(body.containsKey("quality"));
        assertTrue(body.containsKey("bugs"));
        assertTrue(body.containsKey("codeReviews"));
        assertTrue(body.containsKey("system"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> webhookMetrics = (Map<String, Object>) body.get("webhooks");
        assertEquals(10.0, webhookMetrics.get("received"));
        assertEquals(8.0, webhookMetrics.get("processed"));
        assertEquals(2.0, webhookMetrics.get("failed"));
    }
    
    @Test
    public void testGetCategoryMetrics() {
        // Given - add some test metrics for webhook category
        meterRegistry.counter("webhook.received.total").increment(5);
        meterRegistry.counter("webhook.processed.total").increment(4);
        meterRegistry.timer("webhook.processing.duration").record(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getCategoryMetrics("webhook");
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals(5.0, body.get("webhook.received.total"));
        assertEquals(4.0, body.get("webhook.processed.total"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> timerData = (Map<String, Object>) body.get("webhook.processing.duration");
        assertNotNull(timerData);
        assertEquals(1L, timerData.get("count"));
        assertTrue((Double) timerData.get("totalTime") > 0);
    }
    
    @Test
    public void testGetCircuitBreakerStatus() {
        // Given
        CircuitBreakerConfig.CircuitBreaker mockCircuitBreaker = mock(CircuitBreakerConfig.CircuitBreaker.class);
        when(mockCircuitBreaker.getState()).thenReturn(CircuitBreakerConfig.State.CLOSED);
        when(mockCircuitBreaker.getFailureCount()).thenReturn(0);
        when(circuitBreakerRegistry.getCircuitBreaker(anyString())).thenReturn(mockCircuitBreaker);
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getCircuitBreakerStatus();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertTrue(body.containsKey("sonarqube"));
        assertTrue(body.containsKey("gitlab-api"));
        assertTrue(body.containsKey("external-service"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sonarqubeStatus = (Map<String, Object>) body.get("sonarqube");
        assertEquals("CLOSED", sonarqubeStatus.get("state"));
        assertEquals(0, sonarqubeStatus.get("failureCount"));
    }
    
    @Test
    public void testGetCircuitBreakerStatusWithException() {
        // Given
        when(circuitBreakerRegistry.getCircuitBreaker(anyString()))
            .thenThrow(new RuntimeException("Circuit breaker not found"));
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getCircuitBreakerStatus();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> sonarqubeStatus = (Map<String, Object>) body.get("sonarqube");
        assertEquals("UNKNOWN", sonarqubeStatus.get("state"));
        assertEquals("Circuit breaker not found", sonarqubeStatus.get("error"));
    }
    
    @Test
    public void testGetHealthSummary() {
        // Given
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("db", Health.up().build());
        healthDetails.put("redis", Health.up().build());
        healthDetails.put("diskSpace", Health.down().build());
        
        Health overallHealth = Health.up().withDetails(healthDetails).build();
        when(healthEndpoint.health()).thenReturn(overallHealth);
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("components"));
        assertTrue(body.containsKey("healthScore"));
        
        // Health score should be 66.67% (2 out of 3 components are UP)
        Double healthScore = (Double) body.get("healthScore");
        assertEquals(66.67, healthScore, 0.01);
    }
    
    @Test
    public void testGetHealthSummaryWithException() {
        // Given
        when(healthEndpoint.health()).thenThrow(new RuntimeException("Health check failed"));
        
        // When
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("DOWN", body.get("status"));
        assertEquals("Health check failed", body.get("error"));
    }
    
    @Test
    public void testTriggerTestWebhookEvent() {
        // When
        ResponseEntity<String> response = monitoringController.triggerTestWebhookEvent();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test webhook event triggered", response.getBody());
        
        // Verify that the business metrics methods were called
        verify(businessMetrics).incrementWebhookReceived("test");
        verify(businessMetrics).incrementWebhookProcessed();
    }
}