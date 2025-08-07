package com.gitlab.metrics.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard webhook response DTO
 */
public class WebhookResponse {
    
    private String status;
    private String message;
    @JsonProperty("processed_at")
    private String processedAt;
    
    // Constructors
    public WebhookResponse() {}
    
    public WebhookResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.processedAt = java.time.LocalDateTime.now().toString();
    }
    
    // Static factory methods
    public static WebhookResponse success() {
        return new WebhookResponse("success", "Webhook processed successfully");
    }
    
    public static WebhookResponse success(String message) {
        return new WebhookResponse("success", message);
    }
    
    public static WebhookResponse error(String message) {
        return new WebhookResponse("error", message);
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}