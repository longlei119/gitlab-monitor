package com.gitlab.metrics.service.webhook;

import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.service.webhook.WebhookEventProcessor.WebhookEventMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebhookEventProcessor单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class WebhookEventProcessorTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WebhookEventProcessor webhookEventProcessor;

    private String requestId;
    private PushEventRequest pushEvent;
    private MergeRequestEventRequest mergeRequestEvent;
    private IssueEventRequest issueEvent;

    @Before
    public void setUp() {
        requestId = "test-request-123";
        pushEvent = createMockPushEvent();
        mergeRequestEvent = createMockMergeRequestEvent();
        issueEvent = createMockIssueEvent();
    }

    @Test
    public void testProcessEventAsync_PushEvent() {
        // When
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebhookEventMessage> messageCaptor = ArgumentCaptor.forClass(WebhookEventMessage.class);

        verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());

        assertEquals(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, queueCaptor.getValue());
        WebhookEventMessage capturedMessage = messageCaptor.getValue();
        assertEquals(requestId, capturedMessage.getRequestId());
        assertEquals(pushEvent, capturedMessage.getEventData());
        assertTrue(capturedMessage.getTimestamp() > 0);
    }

    @Test
    public void testProcessEventAsync_PushEvent_MergeCommit() {
        // Given - 创建包含合并提交的push事件
        PushEventRequest.CommitInfo mergeCommit = new PushEventRequest.CommitInfo();
        mergeCommit.setMessage("Merge branch 'feature' into 'main'");
        pushEvent.setCommits(Arrays.asList(mergeCommit));

        // When
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then - 应该发送到两个队列
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), any(WebhookEventMessage.class));

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate, times(2)).convertAndSend(queueCaptor.capture(), any(WebhookEventMessage.class));

        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE));
        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE));
    }

    @Test
    public void testProcessEventAsync_MergeRequestEvent() {
        // When
        webhookEventProcessor.processEventAsync("merge request", mergeRequestEvent, requestId);

        // Then
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebhookEventMessage> messageCaptor = ArgumentCaptor.forClass(WebhookEventMessage.class);

        verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());

        assertEquals(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE, queueCaptor.getValue());
        WebhookEventMessage capturedMessage = messageCaptor.getValue();
        assertEquals(requestId, capturedMessage.getRequestId());
        assertEquals(mergeRequestEvent, capturedMessage.getEventData());
    }

    @Test
    public void testProcessEventAsync_MergeRequestEvent_Merged() {
        // Given - 设置为已合并状态
        mergeRequestEvent.getObjectAttributes().setState("merged");

        // When
        webhookEventProcessor.processEventAsync("merge request", mergeRequestEvent, requestId);

        // Then - 应该发送到两个队列
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), any(WebhookEventMessage.class));

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate, times(2)).convertAndSend(queueCaptor.capture(), any(WebhookEventMessage.class));

        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE));
        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.QUALITY_ANALYSIS_QUEUE));
    }

    @Test
    public void testProcessEventAsync_IssueEvent() {
        // When
        webhookEventProcessor.processEventAsync("issue", issueEvent, requestId);

        // Then
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebhookEventMessage> messageCaptor = ArgumentCaptor.forClass(WebhookEventMessage.class);

        verify(rabbitTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());

        assertEquals(RabbitMQConfig.BUG_TRACKING_ANALYSIS_QUEUE, queueCaptor.getValue());
        WebhookEventMessage capturedMessage = messageCaptor.getValue();
        assertEquals(requestId, capturedMessage.getRequestId());
        assertEquals(issueEvent, capturedMessage.getEventData());
    }

    @Test
    public void testProcessEventAsync_IssueEvent_Closed() {
        // Given - 设置为已关闭状态
        issueEvent.getObjectAttributes().setState("closed");

        // When
        webhookEventProcessor.processEventAsync("issue", issueEvent, requestId);

        // Then - 应该发送到两个队列
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), any(WebhookEventMessage.class));

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate, times(2)).convertAndSend(queueCaptor.capture(), any(WebhookEventMessage.class));

        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.BUG_TRACKING_ANALYSIS_QUEUE));
        assertTrue(queueCaptor.getAllValues().contains(RabbitMQConfig.EFFICIENCY_ANALYSIS_QUEUE));
    }

    @Test
    public void testProcessEventAsync_UnsupportedEventType() {
        // When
        webhookEventProcessor.processEventAsync("unsupported", pushEvent, requestId);

        // Then
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    public void testProcessEventAsync_CaseInsensitive() {
        // When - 测试大小写不敏感
        webhookEventProcessor.processEventAsync("PUSH", pushEvent, requestId);

        // Then
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE), any(WebhookEventMessage.class));
    }

    @Test
    public void testProcessEventAsync_RabbitTemplateException() {
        // Given
        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(rabbitTemplate).convertAndSend(anyString(), any());

        // When - 应该不抛出异常，只记录日志
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then
        verify(rabbitTemplate).convertAndSend(anyString(), any(WebhookEventMessage.class));
    }

    @Test
    public void testProcessEventAsync_NullCommits() {
        // Given
        pushEvent.setCommits(null);

        // When
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then - 应该只发送到一个队列
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(WebhookEventMessage.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE), any(WebhookEventMessage.class));
    }

    @Test
    public void testProcessEventAsync_EmptyCommits() {
        // Given
        pushEvent.setCommits(Collections.emptyList());

        // When
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then - 应该只发送到一个队列
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(WebhookEventMessage.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE), any(WebhookEventMessage.class));
    }

    @Test
    public void testProcessEventAsync_MultipleCommits() {
        // Given - 多个提交，但不是合并提交
        PushEventRequest.CommitInfo commit1 = new PushEventRequest.CommitInfo();
        commit1.setMessage("Add feature A");
        PushEventRequest.CommitInfo commit2 = new PushEventRequest.CommitInfo();
        commit2.setMessage("Fix bug B");
        pushEvent.setCommits(Arrays.asList(commit1, commit2));

        // When
        webhookEventProcessor.processEventAsync("push", pushEvent, requestId);

        // Then - 应该只发送到一个队列（因为不是单个合并提交）
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(WebhookEventMessage.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE), any(WebhookEventMessage.class));
    }

    @Test
    public void testWebhookEventMessage() {
        // Test WebhookEventMessage class
        WebhookEventMessage message = new WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(pushEvent);
        message.setTimestamp(12345L);

        assertEquals(requestId, message.getRequestId());
        assertEquals(pushEvent, message.getEventData());
        assertEquals(12345L, message.getTimestamp());
    }

    // Helper methods

    private PushEventRequest createMockPushEvent() {
        PushEventRequest event = new PushEventRequest();
        event.setProjectId(1L);
        event.setRef("refs/heads/main");
        
        PushEventRequest.CommitInfo commit = new PushEventRequest.CommitInfo();
        commit.setId("abc123");
        commit.setMessage("Add new feature");
        event.setCommits(Arrays.asList(commit));
        
        return event;
    }

    private MergeRequestEventRequest createMockMergeRequestEvent() {
        MergeRequestEventRequest event = new MergeRequestEventRequest();
        
        MergeRequestEventRequest.MergeRequestAttributes attrs = new MergeRequestEventRequest.MergeRequestAttributes();
        attrs.setId(123L);
        attrs.setAction("open");
        attrs.setState("opened");
        event.setObjectAttributes(attrs);
        
        return event;
    }

    private IssueEventRequest createMockIssueEvent() {
        IssueEventRequest event = new IssueEventRequest();
        
        IssueEventRequest.IssueAttributes attrs = new IssueEventRequest.IssueAttributes();
        attrs.setId(456L);
        attrs.setAction("open");
        attrs.setState("opened");
        event.setObjectAttributes(attrs);
        
        return event;
    }
}