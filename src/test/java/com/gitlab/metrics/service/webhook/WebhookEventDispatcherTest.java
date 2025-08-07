package com.gitlab.metrics.service.webhook;

import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.exception.WebhookProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WebhookEventDispatcherTest {
    
    @Mock
    private WebhookEventParser<PushEventRequest> pushEventParser;
    
    @Mock
    private WebhookEventProcessor eventProcessor;
    
    @InjectMocks
    private WebhookEventDispatcher dispatcher;
    
    private List<WebhookEventParser<?>> eventParsers;
    private String validPayload;
    private PushEventRequest mockPushEvent;
    
    @Before
    public void setUp() {
        eventParsers = Arrays.asList(pushEventParser);
        dispatcher.eventParsers = eventParsers;
        
        validPayload = "{\"object_kind\":\"push\",\"project_id\":123}";
        mockPushEvent = new PushEventRequest();
        mockPushEvent.setProjectId(123L);
        
        // Setup mock parser
        when(pushEventParser.getEventType()).thenReturn("push");
        when(pushEventParser.isValidPayload(validPayload)).thenReturn(true);
        when(pushEventParser.parseEvent(validPayload)).thenReturn(mockPushEvent);
        
        // Initialize the dispatcher
        dispatcher.initializeParsers();
    }
    
    @Test
    public void testDispatchEvent_Success() {
        // Given
        String eventType = "push";
        String requestId = "test-request-123";
        
        // When
        dispatcher.dispatchEvent(eventType, validPayload, requestId);
        
        // Then
        verify(pushEventParser).isValidPayload(validPayload);
        verify(pushEventParser).parseEvent(validPayload);
        verify(eventProcessor).processEventAsync(eq("push"), eq(mockPushEvent), eq(requestId));
    }
    
    @Test
    public void testDispatchEvent_PushHookEventType() {
        // Given
        String eventType = "Push Hook";
        String requestId = "test-request-123";
        
        // When
        dispatcher.dispatchEvent(eventType, validPayload, requestId);
        
        // Then
        verify(pushEventParser).isValidPayload(validPayload);
        verify(pushEventParser).parseEvent(validPayload);
        verify(eventProcessor).processEventAsync(eq("push"), eq(mockPushEvent), eq(requestId));
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_EmptyEventType() {
        // When & Then
        dispatcher.dispatchEvent("", validPayload, "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_NullEventType() {
        // When & Then
        dispatcher.dispatchEvent(null, validPayload, "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_EmptyPayload() {
        // When & Then
        dispatcher.dispatchEvent("push", "", "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_NullPayload() {
        // When & Then
        dispatcher.dispatchEvent("push", null, "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_UnsupportedEventType() {
        // When & Then
        dispatcher.dispatchEvent("unsupported", validPayload, "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_InvalidPayload() {
        // Given
        when(pushEventParser.isValidPayload(validPayload)).thenReturn(false);
        
        // When & Then
        dispatcher.dispatchEvent("push", validPayload, "test-request");
    }
    
    @Test(expected = WebhookProcessingException.class)
    public void testDispatchEvent_ParsingError() {
        // Given
        when(pushEventParser.parseEvent(validPayload))
            .thenThrow(new RuntimeException("Parsing error"));
        
        // When & Then
        dispatcher.dispatchEvent("push", validPayload, "test-request");
    }
    
    @Test
    public void testGetSupportedEventTypes() {
        // When
        String[] supportedTypes = dispatcher.getSupportedEventTypes();
        
        // Then
        assertNotNull(supportedTypes);
        assertEquals(1, supportedTypes.length);
        assertEquals("push", supportedTypes[0]);
    }
    
    @Test
    public void testIsEventTypeSupported() {
        // Test supported types
        assertTrue(dispatcher.isEventTypeSupported("push"));
        assertTrue(dispatcher.isEventTypeSupported("Push"));
        assertTrue(dispatcher.isEventTypeSupported("PUSH"));
        assertTrue(dispatcher.isEventTypeSupported("Push Hook"));
        
        // Test unsupported types
        assertFalse(dispatcher.isEventTypeSupported("unsupported"));
        assertFalse(dispatcher.isEventTypeSupported(""));
        assertFalse(dispatcher.isEventTypeSupported(null));
    }
    
    @Test
    public void testInitializeParsers() {
        // Given
        WebhookEventDispatcher newDispatcher = new WebhookEventDispatcher();
        newDispatcher.eventParsers = eventParsers;
        
        // When
        newDispatcher.initializeParsers();
        
        // Then
        assertTrue(newDispatcher.isEventTypeSupported("push"));
    }
}