package com.gitlab.metrics.exception;

import com.gitlab.metrics.dto.webhook.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global exception handler for the application
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles webhook validation exceptions
     */
    @ExceptionHandler(WebhookValidationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<WebhookResponse> handleWebhookValidation(WebhookValidationException e) {
        logger.error("Webhook validation failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(WebhookResponse.error("Webhook validation failed: " + e.getMessage()));
    }
    
    /**
     * Handles webhook processing exceptions
     */
    @ExceptionHandler(WebhookProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WebhookResponse> handleWebhookProcessing(WebhookProcessingException e) {
        logger.error("Webhook processing failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(WebhookResponse.error("Webhook processing failed: " + e.getMessage()));
    }
    
    /**
     * Handles general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WebhookResponse> handleRuntimeException(RuntimeException e) {
        logger.error("Unexpected runtime error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(WebhookResponse.error("Internal server error"));
    }
    
    /**
     * Handles general exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WebhookResponse> handleGenericException(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(WebhookResponse.error("Internal server error"));
    }
}