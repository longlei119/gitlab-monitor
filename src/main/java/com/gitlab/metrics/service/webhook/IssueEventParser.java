package com.gitlab.metrics.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parser for GitLab issue events
 */
@Component
public class IssueEventParser implements WebhookEventParser<IssueEventRequest> {
    
    private static final Logger logger = LoggerFactory.getLogger(IssueEventParser.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public IssueEventRequest parseEvent(String payload) {
        try {
            logger.debug("Parsing issue event payload");
            IssueEventRequest event = objectMapper.readValue(payload, IssueEventRequest.class);
            
            // Validate required fields
            if (event.getObjectAttributes() == null) {
                throw new WebhookProcessingException("Missing object_attributes in issue event");
            }
            
            if (event.getObjectAttributes().getId() == null) {
                throw new WebhookProcessingException("Missing issue ID in event");
            }
            
            logger.debug("Successfully parsed issue event for issue: {}, action: {}", 
                        event.getObjectAttributes().getId(), 
                        event.getObjectAttributes().getAction());
            
            return event;
            
        } catch (Exception e) {
            logger.error("Failed to parse issue event payload", e);
            throw new WebhookProcessingException("Failed to parse issue event: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getEventType() {
        return "issue";
    }
    
    @Override
    public boolean isValidPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return false;
        }
        
        try {
            // Quick validation by checking for required fields
            return payload.contains("\"object_kind\":\"issue\"") && 
                   payload.contains("\"object_attributes\"");
        } catch (Exception e) {
            logger.debug("Invalid issue event payload: {}", e.getMessage());
            return false;
        }
    }
}