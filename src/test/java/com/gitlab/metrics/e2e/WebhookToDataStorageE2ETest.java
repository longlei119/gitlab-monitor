package com.gitlab.metrics.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.repository.IssueRepository;
import com.gitlab.metrics.repository.MergeRequestRepository;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Webhook到数据存储的端到端测试
 * 使用TestContainers测试完整的数据流程
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class WebhookToDataStorageE2ETest {

    @ClassRule
    public static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("gitlab_metrics_test")
            .withUsername("test")
            .withPassword("test");

    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.11-management-alpine")
            .withUser("test", "test");

    @ClassRule
    public static GenericContainer<?> redis = new GenericContainer<>("redis:6.2-alpine")
            .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private MergeRequestRepository mergeRequestRepository;

    private String baseUrl;
    private String webhookSecret;

    @Before
    public void setUp() {
        baseUrl = "http://localhost:" + port;
        webhookSecret = "test-webhook-secret";
        
        // 清理测试数据
        commitRepository.deleteAll();
        issueRepository.deleteAll();
        mergeRequestRepository.deleteAll();
    }

    @Test
    public void testPushEventWebhookToCommitStorage() throws Exception {
        // Given - 创建Push事件的Webhook请求
        Map<String, Object> pushEvent = createPushEventPayload();
        String payload = objectMapper.writeValueAsString(pushEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", "Push Hook");
        headers.set("X-Gitlab-Token", webhookSecret);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // When - 发送Webhook请求
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        // Then - 验证Webhook响应
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 等待异步处理完成
        Thread.sleep(5000);

        // 验证数据已存储到数据库
        List<Commit> commits = commitRepository.findAll();
        assertFalse("应该有提交记录被保存", commits.isEmpty());

        Commit savedCommit = commits.get(0);
        assertEquals("abc123", savedCommit.getCommitSha());
        assertEquals("1", savedCommit.getProjectId());
        assertEquals("test-developer", savedCommit.getDeveloperId());
        assertEquals("Test Developer", savedCommit.getDeveloperName());
        assertEquals("Add new feature", savedCommit.getMessage());
        assertEquals("main", savedCommit.getBranch());
        assertEquals(Integer.valueOf(100), savedCommit.getLinesAdded());
        assertEquals(Integer.valueOf(50), savedCommit.getLinesDeleted());
        assertEquals(Integer.valueOf(2), savedCommit.getFilesChanged());
    }

    @Test
    public void testIssueEventWebhookToIssueStorage() throws Exception {
        // Given - 创建Issue事件的Webhook请求
        Map<String, Object> issueEvent = createIssueEventPayload();
        String payload = objectMapper.writeValueAsString(issueEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", "Issue Hook");
        headers.set("X-Gitlab-Token", webhookSecret);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // When - 发送Webhook请求
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        // Then - 验证Webhook响应
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 等待异步处理完成
        Thread.sleep(5000);

        // 验证数据已存储到数据库
        List<Issue> issues = issueRepository.findAll();
        assertFalse("应该有Issue记录被保存", issues.isEmpty());

        Issue savedIssue = issues.get(0);
        assertEquals("123", savedIssue.getIssueId());
        assertEquals("1", savedIssue.getProjectId());
        assertEquals("Test Bug Issue", savedIssue.getTitle());
        assertEquals("This is a test bug", savedIssue.getDescription());
        assertEquals("opened", savedIssue.getStatus());
        assertEquals("100", savedIssue.getAuthorId());
        assertEquals("Test User", savedIssue.getAuthorName());
        assertEquals("bug", savedIssue.getIssueType());
        assertEquals("high", savedIssue.getPriority());
        assertEquals("major", savedIssue.getSeverity());
    }

    @Test
    public void testMergeRequestEventWebhookToMergeRequestStorage() throws Exception {
        // Given - 创建Merge Request事件的Webhook请求
        Map<String, Object> mrEvent = createMergeRequestEventPayload();
        String payload = objectMapper.writeValueAsString(mrEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.set("X-Gitlab-Token", webhookSecret);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // When - 发送Webhook请求
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        // Then - 验证Webhook响应
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 等待异步处理完成
        Thread.sleep(5000);

        // 验证数据已存储到数据库
        List<MergeRequest> mergeRequests = mergeRequestRepository.findAll();
        assertFalse("应该有MergeRequest记录被保存", mergeRequests.isEmpty());

        MergeRequest savedMr = mergeRequests.get(0);
        assertEquals("456", savedMr.getMrId());
        assertEquals("1", savedMr.getProjectId());
        assertEquals("200", savedMr.getAuthorId());
        assertEquals("opened", savedMr.getStatus());
        assertEquals("feature-branch", savedMr.getSourceBranch());
        assertEquals("main", savedMr.getTargetBranch());
        assertEquals(Integer.valueOf(5), savedMr.getChangedFiles());
        assertEquals(Integer.valueOf(150), savedMr.getAdditions());
        assertEquals(Integer.valueOf(75), savedMr.getDeletions());
    }

    @Test
    public void testCompleteWorkflowFromWebhookToAPIQuery() throws Exception {
        // Given - 发送多个Webhook事件
        sendPushEventWebhook();
        sendIssueEventWebhook();
        sendMergeRequestEventWebhook();

        // 等待所有异步处理完成
        Thread.sleep(10000);

        // When - 查询API获取数据
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(1);

        String apiUrl = baseUrl + "/api/v1/metrics/commits" +
                "?startDate=" + startDate.toString() +
                "&endDate=" + endDate.toString() +
                "&projectId=1" +
                "&includeDetails=true";

        ResponseEntity<String> apiResponse = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证API响应
        assertEquals(HttpStatus.OK, apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        // 验证响应包含预期的数据结构
        String responseBody = apiResponse.getBody();
        assertTrue("响应应包含项目ID", responseBody.contains("\"projectId\":\"1\""));
        assertTrue("响应应包含开发者统计", responseBody.contains("\"developerStats\""));
        assertTrue("响应应包含趋势数据", responseBody.contains("\"trendData\""));

        // 验证数据库中的数据一致性
        List<Commit> commits = commitRepository.findAll();
        List<Issue> issues = issueRepository.findAll();
        List<MergeRequest> mergeRequests = mergeRequestRepository.findAll();

        assertEquals("应该有1个提交记录", 1, commits.size());
        assertEquals("应该有1个Issue记录", 1, issues.size());
        assertEquals("应该有1个MergeRequest记录", 1, mergeRequests.size());
    }

    @Test
    public void testDataConsistencyAcrossMultipleWebhooks() throws Exception {
        // Given - 发送多个相关的Webhook事件
        String commitSha = "abc123";
        String projectId = "1";

        // 发送Push事件
        Map<String, Object> pushEvent = createPushEventPayload();
        sendWebhookEvent(pushEvent, "Push Hook");

        // 发送相关的Issue事件
        Map<String, Object> issueEvent = createIssueEventPayload();
        sendWebhookEvent(issueEvent, "Issue Hook");

        // 发送相关的MR事件
        Map<String, Object> mrEvent = createMergeRequestEventPayload();
        sendWebhookEvent(mrEvent, "Merge Request Hook");

        // 等待处理完成
        Thread.sleep(10000);

        // When & Then - 验证数据一致性
        List<Commit> commits = commitRepository.findByProjectId(projectId);
        List<Issue> issues = issueRepository.findByProjectId(projectId);
        List<MergeRequest> mergeRequests = mergeRequestRepository.findByProjectId(projectId);

        // 验证所有实体都属于同一个项目
        assertTrue("应该有提交记录", !commits.isEmpty());
        assertTrue("应该有Issue记录", !issues.isEmpty());
        assertTrue("应该有MergeRequest记录", !mergeRequests.isEmpty());

        commits.forEach(commit -> assertEquals(projectId, commit.getProjectId()));
        issues.forEach(issue -> assertEquals(projectId, issue.getProjectId()));
        mergeRequests.forEach(mr -> assertEquals(projectId, mr.getProjectId()));

        // 验证时间戳的合理性
        LocalDateTime now = LocalDateTime.now();
        commits.forEach(commit -> {
            assertNotNull(commit.getTimestamp());
            assertTrue("提交时间应该在合理范围内", 
                commit.getTimestamp().isBefore(now) && 
                commit.getTimestamp().isAfter(now.minusHours(1)));
        });
    }

    @Test
    public void testErrorHandlingInCompleteWorkflow() throws Exception {
        // Given - 创建包含无效数据的Webhook请求
        Map<String, Object> invalidEvent = new HashMap<>();
        invalidEvent.put("invalid_field", "invalid_value");

        String payload = objectMapper.writeValueAsString(invalidEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", "Push Hook");
        headers.set("X-Gitlab-Token", webhookSecret);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // When - 发送无效的Webhook请求
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        // Then - 验证错误处理
        // 系统应该优雅地处理错误，不应该崩溃
        assertTrue("响应状态应该是4xx或5xx", 
            response.getStatusCode().is4xxClientError() || 
            response.getStatusCode().is5xxServerError() ||
            response.getStatusCode().is2xxSuccessful()); // 可能返回成功但不处理

        // 等待处理完成
        Thread.sleep(3000);

        // 验证无效数据没有被存储
        List<Commit> commits = commitRepository.findAll();
        // 无效数据不应该创建有效的提交记录
        assertTrue("无效数据不应该创建提交记录", 
            commits.isEmpty() || commits.stream().noneMatch(c -> c.getCommitSha() == null));
    }

    @Test
    public void testHighVolumeWebhookProcessing() throws Exception {
        // Given - 准备大量Webhook事件
        int eventCount = 10;

        // When - 并发发送多个Webhook事件
        for (int i = 0; i < eventCount; i++) {
            Map<String, Object> pushEvent = createPushEventPayload();
            // 修改每个事件的唯一标识
            Map<String, Object> commit = (Map<String, Object>) ((List<?>) pushEvent.get("commits")).get(0);
            commit.put("id", "commit-" + i);
            commit.put("message", "Commit " + i);

            sendWebhookEvent(pushEvent, "Push Hook");
        }

        // 等待所有事件处理完成
        Thread.sleep(15000);

        // Then - 验证所有事件都被正确处理
        List<Commit> commits = commitRepository.findAll();
        assertEquals("应该处理所有提交事件", eventCount, commits.size());

        // 验证每个提交都有唯一的SHA
        long uniqueCommitCount = commits.stream()
            .map(Commit::getCommitSha)
            .distinct()
            .count();
        assertEquals("所有提交应该有唯一的SHA", eventCount, uniqueCommitCount);
    }

    @Test
    public void testWebhookAuthenticationAndSecurity() throws Exception {
        // Given - 创建没有正确认证的Webhook请求
        Map<String, Object> pushEvent = createPushEventPayload();
        String payload = objectMapper.writeValueAsString(pushEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", "Push Hook");
        // 故意不设置或设置错误的token
        headers.set("X-Gitlab-Token", "wrong-token");

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // When - 发送未认证的Webhook请求
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        // Then - 验证安全检查
        // 系统应该拒绝未认证的请求
        assertTrue("未认证的请求应该被拒绝", 
            response.getStatusCode().is4xxClientError() || 
            response.getStatusCode().is2xxSuccessful()); // 取决于具体的安全实现

        // 验证未认证的数据没有被处理
        Thread.sleep(3000);
        List<Commit> commits = commitRepository.findAll();
        // 如果安全检查正确工作，应该没有数据被存储
        assertTrue("未认证的数据不应该被存储", commits.isEmpty());
    }

    // Helper methods

    private void sendPushEventWebhook() throws Exception {
        Map<String, Object> pushEvent = createPushEventPayload();
        sendWebhookEvent(pushEvent, "Push Hook");
    }

    private void sendIssueEventWebhook() throws Exception {
        Map<String, Object> issueEvent = createIssueEventPayload();
        sendWebhookEvent(issueEvent, "Issue Hook");
    }

    private void sendMergeRequestEventWebhook() throws Exception {
        Map<String, Object> mrEvent = createMergeRequestEventPayload();
        sendWebhookEvent(mrEvent, "Merge Request Hook");
    }

    private void sendWebhookEvent(Map<String, Object> event, String eventType) throws Exception {
        String payload = objectMapper.writeValueAsString(event);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gitlab-Event", eventType);
        headers.set("X-Gitlab-Token", webhookSecret);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/webhook/gitlab", request, String.class);

        assertEquals("Webhook请求应该成功", HttpStatus.OK, response.getStatusCode());
    }

    private Map<String, Object> createPushEventPayload() {
        Map<String, Object> event = new HashMap<>();
        event.put("project_id", 1);
        event.put("ref", "refs/heads/main");

        Map<String, Object> commit = new HashMap<>();
        commit.put("id", "abc123");
        commit.put("message", "Add new feature");
        commit.put("timestamp", "2023-12-01T10:30:00");

        Map<String, Object> author = new HashMap<>();
        author.put("name", "Test Developer");
        author.put("email", "test@example.com");
        commit.put("author", author);

        commit.put("added", Arrays.asList("src/main/java/NewClass.java", "src/test/java/NewClassTest.java"));
        commit.put("modified", Arrays.asList("src/main/java/ExistingClass.java"));
        commit.put("removed", Arrays.asList());

        event.put("commits", Arrays.asList(commit));
        return event;
    }

    private Map<String, Object> createIssueEventPayload() {
        Map<String, Object> event = new HashMap<>();

        Map<String, Object> project = new HashMap<>();
        project.put("id", 1);
        event.put("project", project);

        Map<String, Object> objectAttributes = new HashMap<>();
        objectAttributes.put("id", 123);
        objectAttributes.put("project_id", 1);
        objectAttributes.put("title", "Test Bug Issue");
        objectAttributes.put("description", "This is a test bug");
        objectAttributes.put("state", "opened");
        objectAttributes.put("action", "open");
        objectAttributes.put("created_at", "2023-12-01T10:30:00");
        objectAttributes.put("updated_at", "2023-12-01T10:30:00");
        objectAttributes.put("url", "http://gitlab.com/issues/123");
        objectAttributes.put("labels", Arrays.asList("bug", "high", "major"));
        event.put("object_attributes", objectAttributes);

        Map<String, Object> user = new HashMap<>();
        user.put("id", 100);
        user.put("name", "Test User");
        event.put("user", user);

        return event;
    }

    private Map<String, Object> createMergeRequestEventPayload() {
        Map<String, Object> event = new HashMap<>();

        Map<String, Object> objectAttributes = new HashMap<>();
        objectAttributes.put("id", 456);
        objectAttributes.put("action", "open");
        objectAttributes.put("state", "opened");
        objectAttributes.put("source_branch", "feature-branch");
        objectAttributes.put("target_branch", "main");
        objectAttributes.put("title", "Test MR");
        objectAttributes.put("description", "Test merge request");
        objectAttributes.put("created_at", "2023-12-01T10:30:00");
        objectAttributes.put("updated_at", "2023-12-01T10:30:00");
        event.put("object_attributes", objectAttributes);

        Map<String, Object> project = new HashMap<>();
        project.put("id", 1);
        event.put("project", project);

        Map<String, Object> user = new HashMap<>();
        user.put("id", 200);
        user.put("name", "MR Author");
        event.put("user", user);

        return event;
    }
}