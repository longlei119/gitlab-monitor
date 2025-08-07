package com.gitlab.metrics.security;

import com.gitlab.metrics.exception.WebhookValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Validator for GitLab webhook security
 */
@Component
public class WebhookSecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookSecurityValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    @Value("${gitlab.webhook.secret}")
    private String webhookSecret;
    
    /**
     * Validates GitLab webhook signature
     * 
     * @param signature The X-Gitlab-Token header value
     * @param payload The request body
     * @return true if signature is valid
     * @throws WebhookValidationException if validation fails
     */
    public boolean validateWebhook(String signature, String payload) {
        if (!StringUtils.hasText(signature)) {
            logger.warn("Webhook validation failed: Missing signature");
            throw new WebhookValidationException("Missing webhook signature");
        }
        
        if (!StringUtils.hasText(payload)) {
            logger.warn("Webhook validation failed: Empty payload");
            throw new WebhookValidationException("Empty webhook payload");
        }
        
        if (!StringUtils.hasText(webhookSecret)) {
            logger.error("Webhook validation failed: Webhook secret not configured");
            throw new WebhookValidationException("Webhook secret not configured");
        }
        
        try {
            // For GitLab, we can use simple token comparison or HMAC-SHA256
            // GitLab supports both X-Gitlab-Token (simple token) and X-Gitlab-Event with signature
            
            // Simple token validation (most common for GitLab)
            if (signature.equals(webhookSecret)) {
                logger.debug("Webhook validation successful using simple token");
                return true;
            }
            
            // HMAC-SHA256 validation as fallback
            String expectedSignature = calculateHmacSha256(payload, webhookSecret);
            boolean isValid = MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
            if (isValid) {
                logger.debug("Webhook validation successful using HMAC-SHA256");
                return true;
            } else {
                logger.warn("Webhook validation failed: Invalid signature");
                throw new WebhookValidationException("Invalid webhook signature");
            }
            
        } catch (Exception e) {
            logger.error("Webhook validation error", e);
            throw new WebhookValidationException("Webhook validation error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculates HMAC-SHA256 signature
     */
    private String calculateHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
        }
    }
    
    /**
     * Validates event type
     */
    public boolean isValidEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        
        // Supported GitLab event types
        switch (eventType.toLowerCase()) {
            case "push":
            case "push hook":
            case "merge request":
            case "merge request hook":
            case "issue":
            case "issue hook":
            case "pipeline":
            case "pipeline hook":
            case "job":
            case "job hook":
                return true;
            default:
                logger.debug("Unsupported event type: {}", eventType);
                return false;
        }
    }
}