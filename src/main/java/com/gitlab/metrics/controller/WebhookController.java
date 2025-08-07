package com.gitlab.metrics.controller;

import com.gitlab.metrics.dto.webhook.WebhookRequest;
import com.gitlab.metrics.dto.webhook.WebhookResponse;
import com.gitlab.metrics.exception.WebhookProcessingException;
import com.gitlab.metrics.exception.WebhookValidationException;
import com.gitlab.metrics.security.WebhookSecurityValidator;
import com.gitlab.metrics.service.webhook.WebhookEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for handling GitLab webhook events
 */
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    @Autowired
    private WebhookSecurityValidator securityValidator;
    
    @Autowired
    private WebhookEventDispatcher eventDispatcher;
    
    /**
     * Main endpoint for GitLab webhook events
     */
    @PostMapping("/gitlab")
    public ResponseEntity<WebhookResponse> handleGitLabWebhook(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType,
            @RequestHeader(value = "X-Gitlab-Event-UUID", required = false) String eventUuid,
            @RequestBody String payload,
            HttpServletRequest request) {
        
        // Generate request ID for tracking
        String requestId = StringUtils.hasText(eventUuid) ? eventUuid : UUID.randomUUID().toString();
        
        // Set up MDC for logging context
        MDC.put("requestId", requestId);
        MDC.put("eventType", eventType);
        MDC.put("clientIp", getClientIpAddress(request));
        
        try {
            logger.info("Received GitLab webhook: eventType={}, payloadSize={}", 
                       eventType, payload != null ? payload.length() : 0);
            
            // Validate request headers
            validateRequestHeaders(token, eventType);
            
            // Validate webhook signature/token
            securityValidator.validateWebhook(token, payload);
            
            // Validate event type
            if (!securityValidator.isValidEventType(eventType)) {
                logger.warn("Unsupported event type: {}", eventType);
                return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("Unsupported event type: " + eventType));
            }
            
            // Log successful validation
            logger.info("Webhook validation successful for event: {}", eventType);
            
            // Process the webhook (this will be implemented in task 3.2)
            processWebhookEvent(eventType, payload, requestId);
            
            // Return success response
            WebhookResponse response = WebhookResponse.success("Event processed successfully");
            logger.info("Webhook processed successfully: eventType={}, requestId={}", eventType, requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (WebhookValidationException e) {
            logger.error("Webhook validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(WebhookResponse.error("Webhook validation failed: " + e.getMessage()));
                
        } catch (WebhookProcessingException e) {
            logger.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebhookResponse.error("Webhook processing failed: " + e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Unexpected error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebhookResponse.error("Internal server error"));
                
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }
    
    /**
     * Health check endpoint for webhook service
     */
    @GetMapping("/health")
    public ResponseEntity<WebhookResponse> health() {
        return ResponseEntity.ok(WebhookResponse.success("Webhook service is healthy"));
    }
    
    /**
     * Validates required request headers
     */
    private void validateRequestHeaders(String token, String eventType) {
        if (!StringUtils.hasText(token)) {
            throw new WebhookValidationException("Missing X-Gitlab-Token header");
        }
        
        if (!StringUtils.hasText(eventType)) {
            throw new WebhookValidationException("Missing X-Gitlab-Event header");
        }
    }
    
    /**
     * Processes the webhook event using the event dispatcher
     */
    private void processWebhookEvent(String eventType, String payload, String requestId) {
        logger.info("Processing webhook event: eventType={}, requestId={}", eventType, requestId);
        
        // Basic payload validation
        if (!StringUtils.hasText(payload)) {
            throw new WebhookProcessingException("Empty webhook payload");
        }
        
        try {
            // Dispatch event to appropriate parser and processor
            eventDispatcher.dispatchEvent(eventType, payload, requestId);
            
            logger.debug("Webhook event dispatched successfully: eventType={}, requestId={}", eventType, requestId);
            
        } catch (Exception e) {
            logger.error("Failed to dispatch webhook event: eventType={}, requestId={}", eventType, requestId, e);
            throw new WebhookProcessingException("Failed to process webhook event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}