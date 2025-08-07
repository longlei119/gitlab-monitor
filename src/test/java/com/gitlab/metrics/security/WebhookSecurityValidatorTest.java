package com.gitlab.metrics.security;

import com.gitlab.metrics.exception.WebhookValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class WebhookSecurityValidatorTest {
    
    private WebhookSecurityValidator validator;
    private static final String TEST_SECRET = "test-webhook-secret";
    
    @Before
    public void setUp() {
        validator = new WebhookSecurityValidator();
        ReflectionTestUtils.setField(validator, "webhookSecret", TEST_SECRET);
    }
    
    @Test
    public void testValidateWebhook_SimpleTokenSuccess() {
        // Given
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        assertTrue(validator.validateWebhook(TEST_SECRET, payload));
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        validator.validateWebhook(invalidToken, payload);
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_MissingSignature() {
        // Given
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        validator.validateWebhook(null, payload);
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_EmptySignature() {
        // Given
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        validator.validateWebhook("", payload);
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_EmptyPayload() {
        // When & Then
        validator.validateWebhook(TEST_SECRET, "");
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_NullPayload() {
        // When & Then
        validator.validateWebhook(TEST_SECRET, null);
    }
    
    @Test(expected = WebhookValidationException.class)
    public void testValidateWebhook_MissingSecret() {
        // Given
        WebhookSecurityValidator validatorWithoutSecret = new WebhookSecurityValidator();
        ReflectionTestUtils.setField(validatorWithoutSecret, "webhookSecret", "");
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        validatorWithoutSecret.validateWebhook(TEST_SECRET, payload);
    }
    
    @Test
    public void testIsValidEventType_SupportedEvents() {
        // Test all supported event types
        assertTrue(validator.isValidEventType("push"));
        assertTrue(validator.isValidEventType("Push Hook"));
        assertTrue(validator.isValidEventType("merge request"));
        assertTrue(validator.isValidEventType("Merge Request Hook"));
        assertTrue(validator.isValidEventType("issue"));
        assertTrue(validator.isValidEventType("Issue Hook"));
        assertTrue(validator.isValidEventType("pipeline"));
        assertTrue(validator.isValidEventType("Pipeline Hook"));
        assertTrue(validator.isValidEventType("job"));
        assertTrue(validator.isValidEventType("Job Hook"));
    }
    
    @Test
    public void testIsValidEventType_CaseInsensitive() {
        // Test case insensitivity
        assertTrue(validator.isValidEventType("PUSH"));
        assertTrue(validator.isValidEventType("Push"));
        assertTrue(validator.isValidEventType("pUsH"));
        assertTrue(validator.isValidEventType("MERGE REQUEST"));
        assertTrue(validator.isValidEventType("Merge Request"));
    }
    
    @Test
    public void testIsValidEventType_UnsupportedEvents() {
        // Test unsupported event types
        assertFalse(validator.isValidEventType("unsupported"));
        assertFalse(validator.isValidEventType("wiki"));
        assertFalse(validator.isValidEventType("deployment"));
        assertFalse(validator.isValidEventType(""));
        assertFalse(validator.isValidEventType(null));
    }
}