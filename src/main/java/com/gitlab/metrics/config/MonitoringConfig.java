package com.gitlab.metrics.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuator.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuator.health.HealthEndpoint;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.actuator.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for application monitoring and metrics
 */
@Configuration
public class MonitoringConfig {
    
    /**
     * Configure Prometheus meter registry
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    /**
     * Enable @Timed annotation support
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    /**
     * Customize meter registry with common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        return registry -> {
            // Add common tags to all metrics
            registry.config()
                .commonTags(
                    "application", "gitlab-metrics-backend",
                    "environment", environment.getActiveProfiles().length > 0 ? 
                        environment.getActiveProfiles()[0] : "default",
                    "version", getClass().getPackage().getImplementationVersion() != null ? 
                        getClass().getPackage().getImplementationVersion() : "unknown"
                )
                // Add meter filters
                .meterFilter(MeterFilter.deny(id -> {
                    String name = id.getName();
                    // Filter out noisy metrics
                    return name.startsWith("jvm.gc.pause") || 
                           name.startsWith("process.files") ||
                           name.startsWith("system.load.average");
                }))
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", 
                    java.time.Duration.ofSeconds(10)))
                .meterFilter(MeterFilter.maximumExpectedValue("webhook.processing.duration", 
                    java.time.Duration.ofSeconds(30)));
        };
    }
    
    /**
     * Custom info contributor for application information
     */
    @Bean
    public InfoContributor customInfoContributor(Environment environment) {
        return builder -> {
            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("name", "GitLab Metrics Backend");
            appInfo.put("description", "GitLab研发度量系统后端");
            appInfo.put("version", getClass().getPackage().getImplementationVersion() != null ? 
                getClass().getPackage().getImplementationVersion() : "development");
            appInfo.put("build-time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            appInfo.put("java-version", System.getProperty("java.version"));
            appInfo.put("spring-profiles", environment.getActiveProfiles());
            
            Map<String, Object> features = new HashMap<>();
            features.put("webhook-processing", true);
            features.put("code-quality-analysis", true);
            features.put("bug-tracking", true);
            features.put("test-coverage", true);
            features.put("code-review-enforcement", true);
            features.put("metrics-dashboard", true);
            
            builder.withDetail("application", appInfo);
            builder.withDetail("features", features);
        };
    }
    
    /**
     * Custom health indicator for overall application health
     */
    @Bean
    public HealthIndicator applicationHealthIndicator() {
        return () -> {
            // Check critical application components
            Map<String, Object> details = new HashMap<>();
            details.put("status", "Application is running");
            details.put("uptime", getUptime());
            details.put("memory", getMemoryInfo());
            details.put("threads", getThreadInfo());
            
            return org.springframework.boot.actuator.health.Health.up()
                .withDetails(details)
                .build();
        };
    }
    
    private String getUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%d days, %d hours, %d minutes, %d seconds", 
            days, hours % 24, minutes % 60, seconds % 60);
    }
    
    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("max", formatBytes(runtime.maxMemory()));
        memory.put("total", formatBytes(runtime.totalMemory()));
        memory.put("free", formatBytes(runtime.freeMemory()));
        memory.put("used", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        return memory;
    }
    
    private Map<String, Object> getThreadInfo() {
        Map<String, Object> threads = new HashMap<>();
        threads.put("active", Thread.activeCount());
        threads.put("daemon", java.lang.management.ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        threads.put("peak", java.lang.management.ManagementFactory.getThreadMXBean().getPeakThreadCount());
        return threads;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}