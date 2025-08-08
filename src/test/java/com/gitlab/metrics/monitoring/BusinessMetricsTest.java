package com.gitlab.metrics.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Test class for BusinessMetrics
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessMetricsTest {
    
    private BusinessMetrics businessMetrics;
    private MeterRegistry meterRegistry;
    
    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics();
        
        // Use reflection to set the meterRegistry field
        try {
            java.lang.reflect.Field field = BusinessMetrics.class.getDeclaredField("meterRegistry");
            field.setAccessible(true);
            field.set(businessMetrics, meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        businessMetrics.initMetrics();
    }
    
    @Test
    public void testWebhookMetrics() {
        // Test webhook received counter
        businessMetrics.incrementWebhookReceived("push");
        Counter webhookCounter = meterRegistry.find("webhook.received.total").counter();
        assertNotNull(webhookCounter);
        assertEquals(1.0, webhookCounter.count(), 0.01);
        
        // Test webhook processed counter
        businessMetrics.incrementWebhookProcessed();
        Counter processedCounter = meterRegistry.find("webhook.processed.total").counter();
        assertNotNull(processedCounter);
        assertEquals(1.0, processedCounter.count(), 0.01);
        
        // Test webhook failed counter
        businessMetrics.incrementWebhookFailed("validation_error");
        Counter failedCounter = meterRegistry.find("webhook.failed.total").counter();
        assertNotNull(failedCounter);
        assertEquals(1.0, failedCounter.count(), 0.01);
    }
    
    @Test
    public void testWebhookProcessingTimer() {
        Timer.Sample sample = businessMetrics.startWebhookProcessingTimer();
        assertNotNull(sample);
        
        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        businessMetrics.recordWebhookProcessingTime(sample);
        
        Timer timer = meterRegistry.find("webhook.processing.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }
    
    @Test
    public void testCommitAnalysisMetrics() {
        businessMetrics.incrementCommitsAnalyzed();
        Counter commitsCounter = meterRegistry.find("commits.analyzed.total").counter();
        assertNotNull(commitsCounter);
        assertEquals(1.0, commitsCounter.count(), 0.01);
        
        businessMetrics.incrementCommitAnalysisFailed();
        Counter failedCounter = meterRegistry.find("commits.analysis.failed.total").counter();
        assertNotNull(failedCounter);
        assertEquals(1.0, failedCounter.count(), 0.01);
    }
    
    @Test
    public void testQualityMetrics() {
        businessMetrics.incrementQualityAnalysis();
        Counter qualityCounter = meterRegistry.find("quality.analysis.total").counter();
        assertNotNull(qualityCounter);
        assertEquals(1.0, qualityCounter.count(), 0.01);
        
        businessMetrics.incrementQualityIssuesFound(5);
        Counter issuesCounter = meterRegistry.find("quality.issues.found.total").counter();
        assertNotNull(issuesCounter);
        assertEquals(5.0, issuesCounter.count(), 0.01);
    }
    
    @Test
    public void testBugTrackingMetrics() {
        businessMetrics.incrementBugsCreated("high");
        Counter bugsCreatedCounter = meterRegistry.find("bugs.created.total").counter();
        assertNotNull(bugsCreatedCounter);
        assertEquals(1.0, bugsCreatedCounter.count(), 0.01);
        
        businessMetrics.incrementBugsResolved("high");
        Counter bugsResolvedCounter = meterRegistry.find("bugs.resolved.total").counter();
        assertNotNull(bugsResolvedCounter);
        assertEquals(1.0, bugsResolvedCounter.count(), 0.01);
    }
    
    @Test
    public void testCodeReviewMetrics() {
        businessMetrics.incrementCodeReviews();
        Counter reviewsCounter = meterRegistry.find("code.reviews.total").counter();
        assertNotNull(reviewsCounter);
        assertEquals(1.0, reviewsCounter.count(), 0.01);
        
        businessMetrics.incrementCodeReviewsApproved();
        Counter approvedCounter = meterRegistry.find("code.reviews.approved.total").counter();
        assertNotNull(approvedCounter);
        assertEquals(1.0, approvedCounter.count(), 0.01);
    }
    
    @Test
    public void testTestCoverageMetrics() {
        businessMetrics.incrementTestCoverageAnalysis();
        Counter coverageCounter = meterRegistry.find("test.coverage.analysis.total").counter();
        assertNotNull(coverageCounter);
        assertEquals(1.0, coverageCounter.count(), 0.01);
        
        businessMetrics.updateAverageTestCoverage(85);
        // Note: Gauge values are tested differently as they represent current state
        // The actual gauge value testing would require more complex setup
    }
    
    @Test
    public void testCustomMetrics() {
        businessMetrics.recordCustomCounter("custom.test.counter", "Test counter", "tag1", "value1");
        Counter customCounter = meterRegistry.find("custom.test.counter").counter();
        assertNotNull(customCounter);
        assertEquals(1.0, customCounter.count(), 0.01);
        
        businessMetrics.recordCustomTimer("custom.test.timer", "Test timer", 100, "tag1", "value1");
        Timer customTimer = meterRegistry.find("custom.test.timer").timer();
        assertNotNull(customTimer);
        assertEquals(1, customTimer.count());
    }
}