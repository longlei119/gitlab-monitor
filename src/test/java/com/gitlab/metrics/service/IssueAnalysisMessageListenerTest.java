package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.service.IssueAnalysisService.IssueProcessResult;
import com.gitlab.metrics.service.webhook.WebhookEventProcessor.WebhookEventMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IssueAnalysisMessageListener单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class IssueAnalysisMessageListenerTest {

    @Mock
    private IssueAnalysisService issueAnalysisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IssueAnalysisMessageListener messageListener;

    private String testMessage;
    private WebhookEventMessage webhookEventMessage;
    private IssueEventRequest issueEventRequest;
    private IssueProcessResult successResult;
    private IssueProcessResult failureResult;

    @Before
    public void setUp() throws Exception {
        testMessage = "{\"requestId\":\"req123\",\"eventType\":\"issue\",\"eventData\":{\"id\":123}}";
        
        webhookEventMessage = new WebhookEventMessage();
        webhookEventMessage.setRequestId("req123");
        webhookEventMessage.setEventType("issue");
        webhookEventMessage.setEventData(createMockIssueEventData());
        
        issueEventRequest = createMockIssueEventRequest();
        
        successResult = new IssueProcessResult();
        successResult.setSuccess(true);
        successResult.setIssueId("123");
        successResult.setAction("open");
        successResult.setMessage("处理成功");
        
        failureResult = new IssueProcessResult();
        failureResult.setSuccess(false);
        failureResult.setIssueId("123");
        failureResult.setAction("open");
        failureResult.setMessage("处理失败");
    }

    @Test
    public void testHandleIssueAnalysisMessage_Success() throws Exception {
        // Given
        when(objectMapper.readValue(testMessage, WebhookEventMessage.class))
            .thenReturn(webhookEventMessage);
        when(objectMapper.convertValue(any(), eq(IssueEventRequest.class)))
            .thenReturn(issueEventRequest);
        when(issueAnalysisService.processIssueEvent(issueEventRequest))
            .thenReturn(successResult);

        // When
        messageListener.handleIssueAnalysisMessage(testMessage);

        // Then
        verify(objectMapper).readValue(testMessage, WebhookEventMessage.class);
        verify(objectMapper).convertValue(webhookEventMessage.getEventData(), IssueEventRequest.class);
        verify(issueAnalysisService).processIssueEvent(issueEventRequest);
    }

    @Test
    public void testHandleIssueAnalysisMessage_ProcessingFailure() throws Exception {
        // Given
        when(objectMapper.readValue(testMessage, WebhookEventMessage.class))
            .thenReturn(webhookEventMessage);
        when(objectMapper.convertValue(any(), eq(IssueEventRequest.class)))
            .thenReturn(issueEventRequest);
        when(issueAnalysisService.processIssueEvent(issueEventRequest))
            .thenReturn(failureResult);

        // When
        messageListener.handleIssueAnalysisMessage(testMessage);

        // Then
        verify(objectMapper).readValue(testMessage, WebhookEventMessage.class);
        verify(objectMapper).convertValue(webhookEventMessage.getEventData(), IssueEventRequest.class);
        verify(issueAnalysisService).processIssueEvent(issueEventRequest);
    }

    @Test
    public void testHandleIssueAnalysisMessage_JsonParsingException() throws Exception {
        // Given
        when(objectMapper.readValue(testMessage, WebhookEventMessage.class))
            .thenThrow(new RuntimeException("JSON解析失败"));

        // When
        messageListener.handleIssueAnalysisMessage(testMessage);

        // Then
        verify(objectMapper).readValue(testMessage, WebhookEventMessage.class);
        verify(objectMapper, never()).convertValue(any(), eq(IssueEventRequest.class));
        verify(issueAnalysisService, never()).processIssueEvent(any(IssueEventRequest.class));
    }

    @Test
    public void testHandleIssueAnalysisMessage_ConversionException() throws Exception {
        // Given
        when(objectMapper.readValue(testMessage, WebhookEventMessage.class))
            .thenReturn(webhookEventMessage);
        when(objectMapper.convertValue(any(), eq(IssueEventRequest.class)))
            .thenThrow(new RuntimeException("数据转换失败"));

        // When
        messageListener.handleIssueAnalysisMessage(testMessage);

        // Then
        verify(objectMapper).readValue(testMessage, WebhookEventMessage.class);
        verify(objectMapper).convertValue(webhookEventMessage.getEventData(), IssueEventRequest.class);
        verify(issueAnalysisService, never()).processIssueEvent(any(IssueEventRequest.class));
    }

    @Test
    public void testHandleIssueAnalysisMessage_ServiceException() throws Exception {
        // Given
        when(objectMapper.readValue(testMessage, WebhookEventMessage.class))
            .thenReturn(webhookEventMessage);
        when(objectMapper.convertValue(any(), eq(IssueEventRequest.class)))
            .thenReturn(issueEventRequest);
        when(issueAnalysisService.processIssueEvent(issueEventRequest))
            .thenThrow(new RuntimeException("服务处理异常"));

        // When
        messageListener.handleIssueAnalysisMessage(testMessage);

        // Then
        verify(objectMapper).readValue(testMessage, WebhookEventMessage.class);
        verify(objectMapper).convertValue(webhookEventMessage.getEventData(), IssueEventRequest.class);
        verify(issueAnalysisService).processIssueEvent(issueEventRequest);
    }

    @Test
    public void testHandleIssueAnalysisMessage_EmptyMessage() throws Exception {
        // Given
        String emptyMessage = "";
        when(objectMapper.readValue(emptyMessage, WebhookEventMessage.class))
            .thenThrow(new RuntimeException("空消息"));

        // When
        messageListener.handleIssueAnalysisMessage(emptyMessage);

        // Then
        verify(objectMapper).readValue(emptyMessage, WebhookEventMessage.class);
        verify(issueAnalysisService, never()).processIssueEvent(any(IssueEventRequest.class));
    }

    @Test
    public void testHandleIssueAnalysisMessage_NullMessage() throws Exception {
        // Given
        String nullMessage = null;
        when(objectMapper.readValue(nullMessage, WebhookEventMessage.class))
            .thenThrow(new RuntimeException("空消息"));

        // When
        messageListener.handleIssueAnalysisMessage(nullMessage);

        // Then
        verify(objectMapper).readValue(nullMessage, WebhookEventMessage.class);
        verify(issueAnalysisService, never()).processIssueEvent(any(IssueEventRequest.class));
    }

    // Helper methods

    private Object createMockIssueEventData() {
        // 返回一个模拟的事件数据对象
        return new Object();
    }

    private IssueEventRequest createMockIssueEventRequest() {
        IssueEventRequest request = new IssueEventRequest();
        
        IssueEventRequest.Project project = new IssueEventRequest.Project();
        project.setId(1L);
        request.setProject(project);
        
        IssueEventRequest.IssueAttributes attrs = new IssueEventRequest.IssueAttributes();
        attrs.setId(123L);
        attrs.setProjectId(1L);
        attrs.setTitle("Test Issue");
        attrs.setDescription("Test Description");
        attrs.setState("opened");
        attrs.setAction("open");
        attrs.setCreatedAt("2023-12-01T10:30:00");
        attrs.setUpdatedAt("2023-12-01T10:30:00");
        attrs.setUrl("http://gitlab.com/issues/123");
        request.setObjectAttributes(attrs);
        
        IssueEventRequest.User user = new IssueEventRequest.User();
        user.setId(100L);
        user.setName("Test User");
        request.setUser(user);
        
        return request;
    }
}