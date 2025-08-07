package com.gitlab.metrics.controller;

import com.gitlab.metrics.exception.WebhookValidationException;
import com.gitlab.metrics.security.WebhookSecurityValidator;
import com.gitlab.metrics.service.webhook.WebhookEventDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class WebhookControllerTest {
    
    @Mock
    private WebhookSecurityValidator securityValidator;
    
    @Mock
    private WebhookEventDispatcher eventDispatcher;
    
    @InjectMocks
    private WebhookController webhookController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    public void testHandleGitLabWebhook_Success() throws Exception {
        // Given
        String token = "test-token";
        String eventType = "push";
        String payload = "{\"object_kind\":\"push\",\"project\":{\"id\":1}}";
        
        when(securityValidator.validateWebhook(token, payload)).thenReturn(true);
        when(securityValidator.isValidEventType(eventType)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Token", token)
                .header("X-Gitlab-Event", eventType)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Event processed successfully"));
        
        verify(securityValidator).validateWebhook(token, payload);
        verify(securityValidator).isValidEventType(eventType);
        verify(eventDispatcher).dispatchEvent(eq(eventType), eq(payload), anyString());
    }
    
    @Test
    public void testHandleGitLabWebhook_MissingToken() throws Exception {
        // Given
        String eventType = "push";
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Event", eventType)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Webhook validation failed: Missing X-Gitlab-Token header"));
        
        verify(securityValidator, never()).validateWebhook(anyString(), anyString());
    }
    
    @Test
    public void testHandleGitLabWebhook_MissingEventType() throws Exception {
        // Given
        String token = "test-token";
        String payload = "{\"object_kind\":\"push\"}";
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Webhook validation failed: Missing X-Gitlab-Event header"));
        
        verify(securityValidator, never()).validateWebhook(anyString(), anyString());
    }
    
    @Test
    public void testHandleGitLabWebhook_InvalidSignature() throws Exception {
        // Given
        String token = "invalid-token";
        String eventType = "push";
        String payload = "{\"object_kind\":\"push\"}";
        
        when(securityValidator.validateWebhook(token, payload))
            .thenThrow(new WebhookValidationException("Invalid webhook signature"));
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Token", token)
                .header("X-Gitlab-Event", eventType)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Webhook validation failed: Invalid webhook signature"));
        
        verify(securityValidator).validateWebhook(token, payload);
    }
    
    @Test
    public void testHandleGitLabWebhook_UnsupportedEventType() throws Exception {
        // Given
        String token = "test-token";
        String eventType = "unsupported";
        String payload = "{\"object_kind\":\"unsupported\"}";
        
        when(securityValidator.validateWebhook(token, payload)).thenReturn(true);
        when(securityValidator.isValidEventType(eventType)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Token", token)
                .header("X-Gitlab-Event", eventType)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Unsupported event type: unsupported"));
        
        verify(securityValidator).validateWebhook(token, payload);
        verify(securityValidator).isValidEventType(eventType);
    }
    
    @Test
    public void testHandleGitLabWebhook_WithEventUuid() throws Exception {
        // Given
        String token = "test-token";
        String eventType = "push";
        String eventUuid = "test-uuid-123";
        String payload = "{\"object_kind\":\"push\"}";
        
        when(securityValidator.validateWebhook(token, payload)).thenReturn(true);
        when(securityValidator.isValidEventType(eventType)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhook/gitlab")
                .header("X-Gitlab-Token", token)
                .header("X-Gitlab-Event", eventType)
                .header("X-Gitlab-Event-UUID", eventUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        
        verify(securityValidator).validateWebhook(token, payload);
        verify(securityValidator).isValidEventType(eventType);
        verify(eventDispatcher).dispatchEvent(eq(eventType), eq(payload), eq(eventUuid));
    }
    
    @Test
    public void testHealthEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/webhook/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Webhook service is healthy"));
    }
}