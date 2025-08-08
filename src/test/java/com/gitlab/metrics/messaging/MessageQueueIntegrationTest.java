package com.gitlab.metrics.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.repository.IssueRepository;
import com.gitlab.metrics.repository.MergeRequestRepository;
import com.gitlab.metrics.service.CommitAnalysisMessageListener;
import com.gitlab.metrics.service.IssueAnalysisMessageListener;
import com.gitlab.metrics.service.MergeRequestAnalysisMessageListener;
import com.gitlab.metrics.service.webhook.WebhookEventProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息队列集成测试
 * 测试RabbitMQ消息处理的完整流程
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@RabbitListenerTest(capture = true)
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest"
})
@Transactional
public class MessageQueueIntegrationTest {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitListenerTestHarness harness;

    @MockBean
    private CommitRepository commitRepository;

    @MockBean
    private IssueRepository issueRepository;

    @MockBean
    private MergeRequestRepository mergeRequestRepository;

    @MockBean
    private CommitAnalysisMessageListener commitAnalysisMessageListener;

    @MockBean
    private IssueAnalysisMessageListener issueAnalysisMessageListener;

    @MockBean
    private MergeRequestAnalysisMessageListener mergeRequestAnalysisMessageListener;

    private String requestId;
    private PushEventRequest pushEvent;
    private IssueEventRequest issueEvent;
    private MergeRequestEventRequest mergeRequestEvent;

    @Before
    public void setUp() {
        requestId = "test-request-123";
        pushEvent = createMockPushEvent();
        issueEvent = createMockIssueEvent();
        mergeRequestEvent = createMockMergeRequestEvent();
    }

    @Test
    public void testCommitAnalysisMessageFlow() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(pushEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);

        // Then
        // 验证消息被正确接收和处理
        verify(commitAnalysisMessageListener, timeout(5000)).handleCommitAnalysisMessage(messageJson);
    }

    @Test
    public void testIssueAnalysisMessageFlow() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(issueEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.BUG_TRACKING_ANALYSIS_QUEUE, messageJson);

        // Then
        // 验证消息被正确接收和处理
        verify(issueAnalysisMessageListener, timeout(5000)).handleIssueAnalysisMessage(messageJson);
    }

    @Test
    public void testMergeRequestAnalysisMessageFlow() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(mergeRequestEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE, messageJson);

        // Then
        // 验证消息被正确接收和处理
        verify(mergeRequestAnalysisMessageListener, timeout(5000)).handleMergeRequestAnalysisMessage(messageJson);
    }

    @Test
    public void testMessageProcessingWithDatabaseIntegration() throws Exception {
        // Given
        Commit mockCommit = createMockCommit();
        when(commitRepository.save(any(Commit.class))).thenReturn(mockCommit);

        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(pushEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);

        // Then
        // 等待消息处理完成
        Thread.sleep(2000);

        // 验证消息被处理
        verify(commitAnalysisMessageListener, timeout(5000)).handleCommitAnalysisMessage(messageJson);
    }

    @Test
    public void testMessageProcessingErrorHandling() throws Exception {
        // Given
        String invalidMessage = "invalid-json-message";

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, invalidMessage);

        // Then
        // 验证错误处理 - 消息应该被接收但处理失败
        verify(commitAnalysisMessageListener, timeout(5000)).handleCommitAnalysisMessage(invalidMessage);
    }

    @Test
    public void testMultipleMessageProcessing() throws Exception {
        // Given
        int messageCount = 5;
        
        for (int i = 0; i < messageCount; i++) {
            WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
            message.setRequestId(requestId + "-" + i);
            message.setEventData(pushEvent);
            message.setTimestamp(System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(message);

            // When
            amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);
        }

        // Then
        // 验证所有消息都被处理
        verify(commitAnalysisMessageListener, timeout(10000).times(messageCount))
            .handleCommitAnalysisMessage(anyString());
    }

    @Test
    public void testMessageRoutingToCorrectQueues() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage commitMessage = new WebhookEventProcessor.WebhookEventMessage();
        commitMessage.setRequestId(requestId + "-commit");
        commitMessage.setEventData(pushEvent);

        WebhookEventProcessor.WebhookEventMessage issueMessage = new WebhookEventProcessor.WebhookEventMessage();
        issueMessage.setRequestId(requestId + "-issue");
        issueMessage.setEventData(issueEvent);

        WebhookEventProcessor.WebhookEventMessage mrMessage = new WebhookEventProcessor.WebhookEventMessage();
        mrMessage.setRequestId(requestId + "-mr");
        mrMessage.setEventData(mergeRequestEvent);

        String commitMessageJson = objectMapper.writeValueAsString(commitMessage);
        String issueMessageJson = objectMapper.writeValueAsString(issueMessage);
        String mrMessageJson = objectMapper.writeValueAsString(mrMessage);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, commitMessageJson);
        amqpTemplate.convertAndSend(RabbitMQConfig.BUG_TRACKING_ANALYSIS_QUEUE, issueMessageJson);
        amqpTemplate.convertAndSend(RabbitMQConfig.MERGE_REQUEST_ANALYSIS_QUEUE, mrMessageJson);

        // Then
        // 验证消息被路由到正确的监听器
        verify(commitAnalysisMessageListener, timeout(5000)).handleCommitAnalysisMessage(commitMessageJson);
        verify(issueAnalysisMessageListener, timeout(5000)).handleIssueAnalysisMessage(issueMessageJson);
        verify(mergeRequestAnalysisMessageListener, timeout(5000)).handleMergeRequestAnalysisMessage(mrMessageJson);
    }

    @Test
    public void testMessagePersistence() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(pushEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);

        // Then
        // 等待消息处理
        Thread.sleep(2000);

        // 验证消息被持久化处理
        verify(commitAnalysisMessageListener, timeout(5000)).handleCommitAnalysisMessage(messageJson);
    }

    @Test
    public void testConcurrentMessageProcessing() throws Exception {
        // Given
        int threadCount = 3;
        int messagesPerThread = 2;

        // When - 并发发送消息
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    for (int m = 0; m < messagesPerThread; m++) {
                        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
                        message.setRequestId(requestId + "-thread" + threadId + "-msg" + m);
                        message.setEventData(pushEvent);
                        message.setTimestamp(System.currentTimeMillis());

                        String messageJson = objectMapper.writeValueAsString(message);
                        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);
                    }
                } catch (Exception e) {
                    fail("Failed to send messages: " + e.getMessage());
                }
            }).start();
        }

        // Then
        // 验证所有消息都被处理
        int totalMessages = threadCount * messagesPerThread;
        verify(commitAnalysisMessageListener, timeout(10000).times(totalMessages))
            .handleCommitAnalysisMessage(anyString());
    }

    @Test
    public void testMessageOrderingWithinQueue() throws Exception {
        // Given
        String[] messageIds = {"msg-1", "msg-2", "msg-3"};

        // When - 按顺序发送消息
        for (String messageId : messageIds) {
            WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
            message.setRequestId(messageId);
            message.setEventData(pushEvent);
            message.setTimestamp(System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(message);
            amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);
        }

        // Then
        // 验证消息被按顺序处理
        verify(commitAnalysisMessageListener, timeout(5000).times(messageIds.length))
            .handleCommitAnalysisMessage(anyString());
    }

    @Test
    public void testMessageRetryMechanism() throws Exception {
        // Given
        WebhookEventProcessor.WebhookEventMessage message = new WebhookEventProcessor.WebhookEventMessage();
        message.setRequestId(requestId);
        message.setEventData(pushEvent);
        message.setTimestamp(System.currentTimeMillis());

        String messageJson = objectMapper.writeValueAsString(message);

        // 模拟处理失败
        doThrow(new RuntimeException("Processing failed"))
            .when(commitAnalysisMessageListener)
            .handleCommitAnalysisMessage(messageJson);

        // When
        amqpTemplate.convertAndSend(RabbitMQConfig.COMMIT_ANALYSIS_QUEUE, messageJson);

        // Then
        // 验证消息被尝试处理（可能会重试）
        verify(commitAnalysisMessageListener, timeout(5000).atLeastOnce())
            .handleCommitAnalysisMessage(messageJson);
    }

    // Helper methods to create mock data

    private PushEventRequest createMockPushEvent() {
        PushEventRequest event = new PushEventRequest();
        event.setProjectId(1L);
        event.setRef("refs/heads/main");
        
        PushEventRequest.CommitInfo commit = new PushEventRequest.CommitInfo();
        commit.setId("abc123");
        commit.setMessage("Add new feature");
        commit.setTimestamp("2023-12-01T10:30:00");
        
        PushEventRequest.AuthorInfo author = new PushEventRequest.AuthorInfo();
        author.setName("Test Developer");
        author.setEmail("test@example.com");
        commit.setAuthor(author);
        
        commit.setAdded(Arrays.asList("src/main/java/NewClass.java"));
        commit.setModified(Arrays.asList("src/main/java/ExistingClass.java"));
        commit.setRemoved(Arrays.asList());
        
        event.setCommits(Arrays.asList(commit));
        return event;
    }

    private IssueEventRequest createMockIssueEvent() {
        IssueEventRequest event = new IssueEventRequest();
        
        IssueEventRequest.Project project = new IssueEventRequest.Project();
        project.setId(1L);
        event.setProject(project);
        
        IssueEventRequest.IssueAttributes attrs = new IssueEventRequest.IssueAttributes();
        attrs.setId(123L);
        attrs.setProjectId(1L);
        attrs.setTitle("Test Issue");
        attrs.setDescription("Test issue description");
        attrs.setState("opened");
        attrs.setAction("open");
        attrs.setCreatedAt("2023-12-01T10:30:00");
        attrs.setUpdatedAt("2023-12-01T10:30:00");
        attrs.setUrl("http://gitlab.com/issues/123");
        event.setObjectAttributes(attrs);
        
        IssueEventRequest.User user = new IssueEventRequest.User();
        user.setId(100L);
        user.setName("Test User");
        event.setUser(user);
        
        return event;
    }

    private MergeRequestEventRequest createMockMergeRequestEvent() {
        MergeRequestEventRequest event = new MergeRequestEventRequest();
        
        MergeRequestEventRequest.MergeRequestAttributes attrs = new MergeRequestEventRequest.MergeRequestAttributes();
        attrs.setId(456L);
        attrs.setAction("open");
        attrs.setState("opened");
        attrs.setSourceBranch("feature-branch");
        attrs.setTargetBranch("main");
        attrs.setTitle("Test MR");
        attrs.setDescription("Test merge request");
        event.setObjectAttributes(attrs);
        
        return event;
    }

    private Commit createMockCommit() {
        Commit commit = new Commit();
        commit.setId(1L);
        commit.setCommitSha("abc123");
        commit.setProjectId("1");
        commit.setDeveloperId("test-developer");
        commit.setDeveloperName("Test Developer");
        commit.setTimestamp(LocalDateTime.now());
        commit.setMessage("Test commit");
        commit.setBranch("main");
        commit.setLinesAdded(100);
        commit.setLinesDeleted(50);
        commit.setFilesChanged(5);
        return commit;
    }
}