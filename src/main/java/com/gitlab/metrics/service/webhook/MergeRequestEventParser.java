package com.gitlab.metrics.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parser for GitLab merge request events
 */
@Component
public class MergeRequestEventParser implements WebhookEventParser<MergeRequestEventRequest> {
    
    private static final Logger logger = LoggerFactory.getLogger(MergeRequestEventParser.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public MergeRequestEventRequest parseEvent(String payload) {
        try {
            logger.debug("Parsing merge request event payload");
            MergeRequestEventRequest event = objectMapper.readValue(payload, MergeRequestEventRequest.class);
            
            // Validate required fields
            if (event.getObjectAttributes() == null) {
                throw new WebhookProcessingException("Missing object_attributes in merge request event");
            }
            
            if (event.getObjectAttributes().getId() == null) {
                throw new WebhookProcessingException("Missing merge request ID in event");
            }
            
            logger.debug("Successfully parsed merge request event for MR: {}, action: {}", 
                        event.getObjectAttributes().getId(), 
                        event.getObjectAttributes().getAction());
            
            return event;
            
        } catch (Exception e) {
            logger.error("Failed to parse merge request event payload", e);
            throw new WebhookProcessingException("Failed to parse merge request event: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getEventType() {
        return "merge request";
    }
    
    @Override
    public boolean isValidPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return false;
        }
        
        try {
            // Quick validation by checking for required fields
            return payload.contains("\"object_kind\":\"merge_request\"") && 
                   payload.contains("\"object_attributes\"");
        } catch (Exception e) {
            logger.debug("Invalid merge request event payload: {}", e.getMessage());
            return false;
        }
    }
}