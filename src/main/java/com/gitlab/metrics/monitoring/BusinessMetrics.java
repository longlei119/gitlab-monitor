package com.gitlab.metrics.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom business metrics for monitoring application performance and business KPIs
 */
@Component
public class BusinessMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Webhook metrics
    private Counter webhookReceivedCounter;
    private Counter webhookProcessedCounter;
    private Counter webhookFailedCounter;
    private Timer webhookProcessingTimer;
    
    // Commit analysis metrics
    private Counter commitsAnalyzedCounter;
    private Counter commitAnalysisFailedCounter;
    private Timer commitAnalysisTimer;
    
    // Quality metrics
    private Counter qualityAnalysisCounter;
    private Counter qualityIssuesFoundCounter;
    private Timer qualityAnalysisTimer;
    
    // Bug tracking metrics
    private Counter bugsCreatedCounter;
    private Counter bugsResolvedCounter;
    private Timer bugResolutionTimer;
    
    // Code review metrics
    private Counter codeReviewsCounter;
    private Counter codeReviewsApprovedCounter;
    private Timer codeReviewTimer;
    
    // Test coverage metrics
    private Counter testCoverageAnalysisCounter;
    private Gauge averageTestCoverageGauge;
    
    // System metrics
    private final AtomicLong activeWebhookConnections = new AtomicLong(0);
    private final AtomicLong totalProcessedEvents = new AtomicLong(0);
    private final AtomicLong currentTestCoverage = new AtomicLong(0);
    
    @PostConstruct
    public void initMetrics() {
        // Webhook metrics
        webhookReceivedCounter = Counter.builder("webhook.received.total")
            .description("Total number of webhooks received")
            .tag("type", "all")
            .register(meterRegistry);
            
        webhookProcessedCounter = Counter.builder("webhook.processed.total")
            .description("Total number of webhooks successfully processed")
            .register(meterRegistry);
            
        webhookFailedCounter = Counter.builder("webhook.failed.total")
            .description("Total number of webhook processing failures")
            .register(meterRegistry);
            
        webhookProcessingTimer = Timer.builder("webhook.processing.duration")
            .description("Time taken to process webhooks")
            .register(meterRegistry);
        
        // Commit analysis metrics
        commitsAnalyzedCounter = Counter.builder("commits.analyzed.total")
            .description("Total number of commits analyzed")
            .register(meterRegistry);
            
        commitAnalysisFailedCounter = Counter.builder("commits.analysis.failed.total")
            .description("Total number of commit analysis failures")
            .register(meterRegistry);
            
        commitAnalysisTimer = Timer.builder("commits.analysis.duration")
            .description("Time taken to analyze commits")
            .register(meterRegistry);
        
        // Quality metrics
        qualityAnalysisCounter = Counter.builder("quality.analysis.total")
            .description("Total number of quality analyses performed")
            .register(meterRegistry);
            
        qualityIssuesFoundCounter = Counter.builder("quality.issues.found.total")
            .description("Total number of quality issues found")
            .register(meterRegistry);
            
        qualityAnalysisTimer = Timer.builder("quality.analysis.duration")
            .description("Time taken for quality analysis")
            .register(meterRegistry);
        
        // Bug tracking metrics
        bugsCreatedCounter = Counter.builder("bugs.created.total")
            .description("Total number of bugs created")
            .register(meterRegistry);
            
        bugsResolvedCounter = Counter.builder("bugs.resolved.total")
            .description("Total number of bugs resolved")
            .register(meterRegistry);
            
        bugResolutionTimer = Timer.builder("bugs.resolution.duration")
            .description("Time taken to resolve bugs")
            .register(meterRegistry);
        
        // Code review metrics
        codeReviewsCounter = Counter.builder("code.reviews.total")
            .description("Total number of code reviews")
            .register(meterRegistry);
            
        codeReviewsApprovedCounter = Counter.builder("code.reviews.approved.total")
            .description("Total number of approved code reviews")
            .register(meterRegistry);
            
        codeReviewTimer = Timer.builder("code.review.duration")
            .description("Time taken for code reviews")
            .register(meterRegistry);
        
        // Test coverage metrics
        testCoverageAnalysisCounter = Counter.builder("test.coverage.analysis.total")
            .description("Total number of test coverage analyses")
            .register(meterRegistry);
        
        // Gauge metrics
        Gauge.builder("webhook.connections.active")
            .description("Number of active webhook connections")
            .register(meterRegistry, activeWebhookConnections, AtomicLong::get);
            
        Gauge.builder("events.processed.total")
            .description("Total number of events processed")
            .register(meterRegistry, totalProcessedEvents, AtomicLong::get);
            
        averageTestCoverageGauge = Gauge.builder("test.coverage.average")
            .description("Average test coverage percentage")
            .register(meterRegistry, currentTestCoverage, AtomicLong::get);
    }
    
    // Webhook metrics methods
    public void incrementWebhookReceived(String eventType) {
        webhookReceivedCounter.increment();
        Counter.builder("webhook.received.by.type")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementWebhookProcessed() {
        webhookProcessedCounter.increment();
        totalProcessedEvents.incrementAndGet();
    }
    
    public void incrementWebhookFailed(String errorType) {
        webhookFailedCounter.increment();
        Counter.builder("webhook.failed.by.type")
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment();
    }
    
    public Timer.Sample startWebhookProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordWebhookProcessingTime(Timer.Sample sample) {
        sample.stop(webhookProcessingTimer);
    }
    
    // Commit analysis metrics methods
    public void incrementCommitsAnalyzed() {
        commitsAnalyzedCounter.increment();
    }
    
    public void incrementCommitAnalysisFailed() {
        commitAnalysisFailedCounter.increment();
    }
    
    public Timer.Sample startCommitAnalysisTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCommitAnalysisTime(Timer.Sample sample) {
        sample.stop(commitAnalysisTimer);
    }
    
    // Quality metrics methods
    public void incrementQualityAnalysis() {
        qualityAnalysisCounter.increment();
    }
    
    public void incrementQualityIssuesFound(int count) {
        qualityIssuesFoundCounter.increment(count);
    }
    
    public Timer.Sample startQualityAnalysisTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordQualityAnalysisTime(Timer.Sample sample) {
        sample.stop(qualityAnalysisTimer);
    }
    
    // Bug tracking metrics methods
    public void incrementBugsCreated(String severity) {
        bugsCreatedCounter.increment();
        Counter.builder("bugs.created.by.severity")
            .tag("severity", severity)
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementBugsResolved(String severity) {
        bugsResolvedCounter.increment();
        Counter.builder("bugs.resolved.by.severity")
            .tag("severity", severity)
            .register(meterRegistry)
            .increment();
    }
    
    public Timer.Sample startBugResolutionTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordBugResolutionTime(Timer.Sample sample) {
        sample.stop(bugResolutionTimer);
    }
    
    // Code review metrics methods
    public void incrementCodeReviews() {
        codeReviewsCounter.increment();
    }
    
    public void incrementCodeReviewsApproved() {
        codeReviewsApprovedCounter.increment();
    }
    
    public Timer.Sample startCodeReviewTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCodeReviewTime(Timer.Sample sample) {
        sample.stop(codeReviewTimer);
    }
    
    // Test coverage metrics methods
    public void incrementTestCoverageAnalysis() {
        testCoverageAnalysisCounter.increment();
    }
    
    public void updateAverageTestCoverage(long coverage) {
        currentTestCoverage.set(coverage);
    }
    
    // System metrics methods
    public void incrementActiveWebhookConnections() {
        activeWebhookConnections.incrementAndGet();
    }
    
    public void decrementActiveWebhookConnections() {
        activeWebhookConnections.decrementAndGet();
    }
    
    // Utility methods for custom metrics
    public void recordCustomCounter(String name, String description, String... tags) {
        Counter.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCustomTimer(String name, String description, long durationMs, String... tags) {
        Timer.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}