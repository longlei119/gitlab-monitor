package com.gitlab.metrics.service.webhook;

import com.gitlab.metrics.dto.webhook.WebhookRequest;

/**
 * Interface for parsing webhook events
 */
public interface WebhookEventParser<T extends WebhookRequest> {
    
    /**
     * Parses the webhook payload into a specific event type
     * 
     * @param payload The raw JSON payload
     * @return Parsed event object
     */
    T parseEvent(String payload);
    
    /**
     * Gets the event type this parser handles
     * 
     * @return Event type string
     */
    String getEventType();
    
    /**
     * Validates if the payload is valid for this event type
     * 
     * @param payload The raw JSON payload
     * @return true if valid
     */
    boolean isValidPayload(String payload);
}