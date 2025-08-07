package com.gitlab.metrics.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parser for GitLab push events
 */
@Component
public class PushEventParser implements WebhookEventParser<PushEventRequest> {
    
    private static final Logger logger = LoggerFactory.getLogger(PushEventParser.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public PushEventRequest parseEvent(String payload) {
        try {
            logger.debug("Parsing push event payload");
            PushEventRequest event = objectMapper.readValue(payload, PushEventRequest.class);
            
            // Validate required fields
            if (event.getProjectId() == null) {
                throw new WebhookProcessingException("Missing project_id in push event");
            }
            
            if (!StringUtils.hasText(event.getRef())) {
                throw new WebhookProcessingException("Missing ref in push event");
            }
            
            logger.debug("Successfully parsed push event for project: {}, ref: {}", 
                        event.getProjectId(), event.getRef());
            
            return event;
            
        } catch (Exception e) {
            logger.error("Failed to parse push event payload", e);
            throw new WebhookProcessingException("Failed to parse push event: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getEventType() {
        return "push";
    }
    
    @Override
    public boolean isValidPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return false;
        }
        
        try {
            // Quick validation by checking for required fields
            return payload.contains("\"object_kind\":\"push\"") && 
                   payload.contains("\"project_id\"") &&
                   payload.contains("\"ref\"");
        } catch (Exception e) {
            logger.debug("Invalid push event payload: {}", e.getMessage());
            return false;
        }
    }
}