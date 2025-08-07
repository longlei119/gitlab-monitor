package com.gitlab.metrics.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PushEventParserTest {
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private PushEventParser parser;
    
    private String validPushPayload;
    private PushEventRequest mockPushEvent;
    
    @Before
    public void setUp() {
        validPushPayload = "{\"object_kind\":\"push\",\"project_id\":123,\"ref\":\"refs/heads/main\"}";
        
        mockPushEvent = new PushEventRequest();
        mockPushEvent.setProjectId(123L);
        mockPushEvent.setRef("refs/heads/main");
    }
    
    @Test
    public void testParseEvent_Success() throws Exception {
        // Given
        when(objectMapper.readValue(eq(validPushPayload), eq(PushEventRequest.class)))
            .thenReturn(mockPushEvent);
        
        // When
        PushEventRequest result = parser.parseEvent(validPushPayload);
        
        // Then
        assertNotNull(result);
        assertEquals(Long.valueOf(123), result.getProjectId());
        assertEquals("refs/heads/main", result.getRef());
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testParseEvent_MissingProjectId() throws Exception {
        // Given
        PushEventRequest invalidEvent = new PushEventRequest();
        invalidEvent.setRef("refs/heads/main");
        // projectId is null
        
        when(objectMapper.readValue(eq(validPushPayload), eq(PushEventRequest.class)))
            .thenReturn(invalidEvent);
        
        // When & Then
        parser.parseEvent(validPushPayload);
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testParseEvent_MissingRef() throws Exception {
        // Given
        PushEventRequest invalidEvent = new PushEventRequest();
        invalidEvent.setProjectId(123L);
        // ref is null
        
        when(objectMapper.readValue(eq(validPushPayload), eq(PushEventRequest.class)))
            .thenReturn(invalidEvent);
        
        // When & Then
        parser.parseEvent(validPushPayload);
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testParseEvent_JsonParsingError() throws Exception {
        // Given
        when(objectMapper.readValue(any(String.class), eq(PushEventRequest.class)))
            .thenThrow(new RuntimeException("JSON parsing error"));
        
        // When & Then
        parser.parseEvent(validPushPayload);
    }
    
    @Test
    public void testGetEventType() {
        // When & Then
        assertEquals("push", parser.getEventType());
    }
    
    @Test
    public void testIsValidPayload_Valid() {
        // When & Then
        assertTrue(parser.isValidPayload(validPushPayload));
    }
    
    @Test
    public void testIsValidPayload_Invalid() {
        // Test various invalid payloads
        assertFalse(parser.isValidPayload(null));
        assertFalse(parser.isValidPayload(""));
        assertFalse(parser.isValidPayload("{\"object_kind\":\"merge_request\"}"));
        assertFalse(parser.isValidPayload("{\"object_kind\":\"push\"}"));
        assertFalse(parser.isValidPayload("{\"project_id\":123}"));
    }
    
    @Test
    public void testIsValidPayload_ValidWithAllFields() {
        // Given
        String completePayload = "{\"object_kind\":\"push\",\"project_id\":123,\"ref\":\"refs/heads/main\",\"commits\":[]}";
        
        // When & Then
        assertTrue(parser.isValidPayload(completePayload));
    }
}