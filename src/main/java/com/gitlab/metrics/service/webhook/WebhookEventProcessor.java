package com.gitlab.metrics.service.webhook;

import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.dto.webhook.WebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for processing webhook events asynchronously
 */
@Service
public class WebhookEventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookEventProcessor.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * Processes webhook event asynchronously
     * 
     * @param eventType The event type
     * @param event The parsed event
     * @param requestId The request ID for tracking
     */
    @Async
    public void processEventAsync(String eventType, WebhookRequest event, String requestId) {
        // Set up MDC for async processing
        MDC.put("requestId", requestId);
        MDC.put("eventType", eventType);
        
        try {
            logger.info("Processing webhook event asynchronously: type={}, requestId={}", eventType, requestId);
            
            switch (eventType.toLowerCase()) {
                case "push":
                    processPushEvent((PushEventRequest) event, requestId);
                    break;
                case "merge request":
                    processMergeRequestEvent((MergeRequestEventRequest) event, requestId);
                    break;
                case "issue":
                    processIssueEvent((IssueEventRequest) event, requestId);
                    break;
                default:
                    logger.warn("Unsupported event type for processing: {}", eventType);
                    return;
            }
            
            logger.info("Successfully processed webhook event: type={}, requestId={}", eventType, requestId);
            
        } catch (Exception e) {
            logger.error("Failed to process webhook event: type={}, requestId={}", eventType, requestId, e);
            // In a real implementation, you might want to send to a dead letter queue
            
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }
    
    /**
     * Processes push events
     */
    private void processPushEvent(PushEventRequest event, String requestId) {
        logger.debug("Processing push event: project={}, ref={}, commits={}", 
                    event.getProjectId(), event.getRef(), 
                    event.getCommits() != null ? event.getCommits().size() : 0);
        
        // Send to commit analysis queue
        sendToQueue(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, event, requestId);
        
        // If this is a merge commit, also send to merge analysis queue
        if (event.getCommits() != null && event.getCommits().size() == 1) {
            PushEventRequest.CommitInfo commit = event.getCommits().get(0);
            if (commit.getMessage() != null && commit.getMessage().startsWith("Merge")) {
                sendToQueue(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE, event, requestId);
            }
        }
        
        logger.debug("Push event processing completed: requestId={}", requestId);
    }
    
    /**
     * Processes merge request events
     */
    private void processMergeRequestEvent(MergeRequestEventRequest event, String requestId) {
        logger.debug("Processing merge request event: id={}, action={}, state={}", 
                    event.getObjectAttributes().getId(),
                    event.getObjectAttributes().getAction(),
                    event.getObjectAttributes().getState());
        
        // Send to code review analysis queue
        sendToQueue(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE, event, requestId);
        
        // If merge request is merged, also trigger quality analysis
        if ("merged".equals(event.getObjectAttributes().getState())) {
            sendToQueue(RabbitMQConfig.QUALITY_ANALYSIS_QUEUE, event, requestId);
        }
        
        logger.debug("Merge request event processing completed: requestId={}", requestId);
    }
    
    /**
     * Processes issue events
     */
    private void processIssueEvent(IssueEventRequest event, String requestId) {
        logger.debug("Processing issue event: id={}, action={}, state={}", 
                    event.getObjectAttributes().getId(),
                    event.getObjectAttributes().getAction(),
                    event.getObjectAttributes().getState());
        
        // Send to bug tracking analysis queue
        sendToQueue(RabbitMQConfig.BUG_TRACKING_ANALYSIS_QUEUE, event, requestId);
        
        // If issue is closed, trigger efficiency analysis
        if ("closed".equals(event.getObjectAttributes().getState())) {
            sendToQueue(RabbitMQConfig.EFFICIENCY_ANALYSIS_QUEUE, event, requestId);
        }
        
        logger.debug("Issue event processing completed: requestId={}", requestId);
    }
    
    /**
     * Sends event to RabbitMQ queue
     */
    private void sendToQueue(String queueName, Object event, String requestId) {
        try {
            logger.debug("Sending event to queue: queue={}, requestId={}", queueName, requestId);
            
            // Add metadata to the message
            WebhookEventMessage message = new WebhookEventMessage();
            message.setRequestId(requestId);
            message.setEventData(event);
            message.setTimestamp(System.currentTimeMillis());
            
            rabbitTemplate.convertAndSend(queueName, message);
            
            logger.debug("Event sent to queue successfully: queue={}, requestId={}", queueName, requestId);
            
        } catch (Exception e) {
            logger.error("Failed to send event to queue: queue={}, requestId={}", queueName, requestId, e);
            // In production, you might want to implement retry logic or dead letter queue
        }
    }
    
    /**
     * Message wrapper for queue events
     */
    public static class WebhookEventMessage {
        private String requestId;
        private Object eventData;
        private long timestamp;
        
        // Getters and Setters
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public Object getEventData() {
            return eventData;
        }
        
        public void setEventData(Object eventData) {
            this.eventData = eventData;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}