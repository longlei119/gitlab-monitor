package com.gitlab.metrics.service.webhook;

import com.gitlab.metrics.dto.webhook.WebhookRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for dispatching webhook events to appropriate parsers and handlers
 */
@Service
public class WebhookEventDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookEventDispatcher.class);
    
    @Autowired
    List<WebhookEventParser<?>> eventParsers;
    
    @Autowired
    private WebhookEventProcessor eventProcessor;
    
    private Map<String, WebhookEventParser<?>> parserMap;
    
    @PostConstruct
    public void initializeParsers() {
        parserMap = new HashMap<>();
        for (WebhookEventParser<?> parser : eventParsers) {
            String eventType = parser.getEventType().toLowerCase();
            parserMap.put(eventType, parser);
            logger.info("Registered webhook parser for event type: {}", eventType);
        }
        logger.info("Initialized {} webhook event parsers", parserMap.size());
    }
    
    /**
     * Dispatches webhook event to appropriate parser and processor
     * 
     * @param eventType The GitLab event type
     * @param payload The raw JSON payload
     * @param requestId The request ID for tracking
     */
    public void dispatchEvent(String eventType, String payload, String requestId) {
        if (!StringUtils.hasText(eventType)) {
            throw new WebhookProcessingException("Event type is required");
        }
        
        if (!StringUtils.hasText(payload)) {
            throw new WebhookProcessingException("Payload is required");
        }
        
        String normalizedEventType = normalizeEventType(eventType);
        logger.debug("Dispatching event: type={}, requestId={}", normalizedEventType, requestId);
        
        // Find appropriate parser
        WebhookEventParser<?> parser = parserMap.get(normalizedEventType);
        if (parser == null) {
            logger.warn("No parser found for event type: {}", normalizedEventType);
            throw new WebhookProcessingException("Unsupported event type: " + eventType);
        }
        
        // Validate payload
        if (!parser.isValidPayload(payload)) {
            logger.error("Invalid payload for event type: {}", normalizedEventType);
            throw new WebhookProcessingException("Invalid payload for event type: " + eventType);
        }
        
        try {
            // Parse event
            WebhookRequest parsedEvent = parser.parseEvent(payload);
            logger.debug("Successfully parsed event: type={}, requestId={}", normalizedEventType, requestId);
            
            // Process event asynchronously
            eventProcessor.processEventAsync(normalizedEventType, parsedEvent, requestId);
            
            logger.info("Event dispatched successfully: type={}, requestId={}", normalizedEventType, requestId);
            
        } catch (Exception e) {
            logger.error("Failed to dispatch event: type={}, requestId={}", normalizedEventType, requestId, e);
            throw new WebhookProcessingException("Failed to dispatch event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Normalizes event type to match parser keys
     */
    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "";
        }
        
        String normalized = eventType.toLowerCase().trim();
        
        // Handle GitLab event type variations
        switch (normalized) {
            case "push hook":
                return "push";
            case "merge request hook":
            case "merge_request":
                return "merge request";
            case "issue hook":
                return "issue";
            case "pipeline hook":
                return "pipeline";
            case "job hook":
                return "job";
            default:
                return normalized;
        }
    }
    
    /**
     * Gets available event types
     */
    public String[] getSupportedEventTypes() {
        return parserMap.keySet().toArray(new String[0]);
    }
    
    /**
     * Checks if event type is supported
     */
    public boolean isEventTypeSupported(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        
        String normalized = normalizeEventType(eventType);
        return parserMap.containsKey(normalized);
    }
}