package com.gitlab.metrics.controller;

import com.gitlab.metrics.config.CircuitBreakerConfig;
import com.gitlab.metrics.monitoring.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.HealthEndpoint;
import org.springframework.boot.actuator.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for monitoring and metrics endpoints
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    @Autowired
    private CircuitBreakerConfig.CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private HealthEndpoint healthEndpoint;
    
    /**
     * Get application metrics summary
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Webhook metrics
        Map<String, Object> webhookMetrics = new HashMap<>();
        webhookMetrics.put("received", getCounterValue("webhook.received.total"));
        webhookMetrics.put("processed", getCounterValue("webhook.processed.total"));
        webhookMetrics.put("failed", getCounterValue("webhook.failed.total"));
        webhookMetrics.put("averageProcessingTime", getTimerMean("webhook.processing.duration"));
        summary.put("webhooks", webhookMetrics);
        
        // Commit metrics
        Map<String, Object> commitMetrics = new HashMap<>();
        commitMetrics.put("analyzed", getCounterValue("commits.analyzed.total"));
        commitMetrics.put("analysisFailed", getCounterValue("commits.analysis.failed.total"));
        commitMetrics.put("averageAnalysisTime", getTimerMean("commits.analysis.duration"));
        summary.put("commits", commitMetrics);
        
        // Quality metrics
        Map<String, Object> qualityMetrics = new HashMap<>();
        qualityMetrics.put("analysisCount", getCounterValue("quality.analysis.total"));
        qualityMetrics.put("issuesFound", getCounterValue("quality.issues.found.total"));
        qualityMetrics.put("averageAnalysisTime", getTimerMean("quality.analysis.duration"));
        summary.put("quality", qualityMetrics);
        
        // Bug metrics
        Map<String, Object> bugMetrics = new HashMap<>();
        bugMetrics.put("created", getCounterValue("bugs.created.total"));
        bugMetrics.put("resolved", getCounterValue("bugs.resolved.total"));
        bugMetrics.put("averageResolutionTime", getTimerMean("bugs.resolution.duration"));
        summary.put("bugs", bugMetrics);
        
        // Code review metrics
        Map<String, Object> reviewMetrics = new HashMap<>();
        reviewMetrics.put("total", getCounterValue("code.reviews.total"));
        reviewMetrics.put("approved", getCounterValue("code.reviews.approved.total"));
        reviewMetrics.put("averageReviewTime", getTimerMean("code.review.duration"));
        summary.put("codeReviews", reviewMetrics);
        
        // System metrics
        Map<String, Object> systemMetrics = new HashMap<>();
        systemMetrics.put("activeConnections", getGaugeValue("webhook.connections.active"));
        systemMetrics.put("totalProcessedEvents", getGaugeValue("events.processed.total"));
        systemMetrics.put("averageTestCoverage", getGaugeValue("test.coverage.average"));
        summary.put("system", systemMetrics);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get detailed metrics for a specific category
     */
    @GetMapping("/metrics/{category}")
    public ResponseEntity<Map<String, Object>> getCategoryMetrics(@PathVariable String category) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get all meters that start with the category name
        Search.in(meterRegistry)
            .name(name -> name.startsWith(category))
            .meters()
            .forEach(meter -> {
                String meterName = meter.getId().getName();
                switch (meter.getId().getType()) {
                    case COUNTER:
                        metrics.put(meterName, ((io.micrometer.core.instrument.Counter) meter).count());
                        break;
                    case TIMER:
                        io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                        Map<String, Object> timerData = new HashMap<>();
                        timerData.put("count", timer.count());
                        timerData.put("totalTime", timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timerData.put("mean", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timerData.put("max", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
                        metrics.put(meterName, timerData);
                        break;
                    case GAUGE:
                        metrics.put(meterName, ((io.micrometer.core.instrument.Gauge) meter).value());
                        break;
                    default:
                        metrics.put(meterName, meter.measure().iterator().next().getValue());
                }
            });
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get circuit breaker status for all services
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // This is a simplified implementation since we don't have direct access to all circuit breakers
        // In a real implementation, you would maintain a registry of all circuit breaker names
        String[] knownServices = {"sonarqube", "gitlab-api", "external-service"};
        
        for (String serviceName : knownServices) {
            try {
                CircuitBreakerConfig.CircuitBreaker cb = circuitBreakerRegistry.getCircuitBreaker(serviceName);
                Map<String, Object> cbStatus = new HashMap<>();
                cbStatus.put("state", cb.getState().toString());
                cbStatus.put("failureCount", cb.getFailureCount());
                status.put(serviceName, cbStatus);
            } catch (Exception e) {
                Map<String, Object> cbStatus = new HashMap<>();
                cbStatus.put("state", "UNKNOWN");
                cbStatus.put("error", e.getMessage());
                status.put(serviceName, cbStatus);
            }
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get health status summary
     */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        Map<String, Object> healthSummary = new HashMap<>();
        
        try {
            var health = healthEndpoint.health();
            healthSummary.put("status", health.getStatus().getCode());
            
            if (health.getDetails() != null) {
                Map<String, String> componentStatus = health.getDetails().entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            if (entry.getValue() instanceof org.springframework.boot.actuator.health.Health) {
                                return ((org.springframework.boot.actuator.health.Health) entry.getValue())
                                    .getStatus().getCode();
                            }
                            return "UNKNOWN";
                        }
                    ));
                healthSummary.put("components", componentStatus);
            }
            
            // Add overall health score
            long healthyComponents = healthSummary.containsKey("components") ? 
                ((Map<String, String>) healthSummary.get("components")).values().stream()
                    .mapToLong(status -> "UP".equals(status) ? 1 : 0)
                    .sum() : 0;
            long totalComponents = healthSummary.containsKey("components") ? 
                ((Map<String, String>) healthSummary.get("components")).size() : 0;
            
            if (totalComponents > 0) {
                double healthScore = (double) healthyComponents / totalComponents * 100;
                healthSummary.put("healthScore", Math.round(healthScore * 100.0) / 100.0);
            }
            
        } catch (Exception e) {
            healthSummary.put("status", "DOWN");
            healthSummary.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(healthSummary);
    }
    
    /**
     * Trigger a test metric event (for testing purposes)
     */
    @GetMapping("/test/webhook-event")
    public ResponseEntity<String> triggerTestWebhookEvent() {
        businessMetrics.incrementWebhookReceived("test");
        businessMetrics.incrementWebhookProcessed();
        return ResponseEntity.ok("Test webhook event triggered");
    }
    
    // Helper methods
    private double getCounterValue(String name) {
        return Search.in(meterRegistry)
            .name(name)
            .counter()
            .map(io.micrometer.core.instrument.Counter::count)
            .orElse(0.0);
    }
    
    private double getTimerMean(String name) {
        return Search.in(meterRegistry)
            .name(name)
            .timer()
            .map(timer -> timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .orElse(0.0);
    }
    
    private double getGaugeValue(String name) {
        return Search.in(meterRegistry)
            .name(name)
            .gauge()
            .map(io.micrometer.core.instrument.Gauge::value)
            .orElse(0.0);
    }
}