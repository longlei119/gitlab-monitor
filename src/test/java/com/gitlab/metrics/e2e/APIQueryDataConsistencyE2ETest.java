package com.gitlab.metrics.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.entity.*;
import com.gitlab.metrics.repository.*;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * API查询数据一致性端到端测试
 * 验证API查询返回的数据与数据库中存储的数据一致性
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class APIQueryDataConsistencyE2ETest {

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
    private QualityMetricsRepository qualityMetricsRepository;

    @Autowired
    private TestCoverageRepository testCoverageRepository;

    @Autowired
    private MergeRequestRepository mergeRequestRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CodeReviewRepository codeReviewRepository;

    @Autowired
    private FileChangeRepository fileChangeRepository;

    private String baseUrl;
    private String projectId;
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;

    @Before
    public void setUp() {
        baseUrl = "http://localhost:" + port;
        projectId = "test-project-1";
        testStartTime = LocalDateTime.now().minusDays(7);
        testEndTime = LocalDateTime.now();

        // 清理测试数据
        cleanupTestData();

        // 创建测试数据
        setupTestData();
    }

    @Test
    public void testCommitStatsAPIDataConsistency() throws Exception {
        // When - 调用提交统计API
        String apiUrl = String.format("%s/api/v1/metrics/commits?startDate=%s&endDate=%s&projectId=%s&includeDetails=true",
                baseUrl, testStartTime.toString(), testEndTime.toString(), projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertEquals(projectId, responseJson.get("projectId").asText());
        assertNotNull(responseJson.get("startDate"));
        assertNotNull(responseJson.get("endDate"));

        // 验证开发者统计数据一致性
        JsonNode developerStats = responseJson.get("developerStats");
        assertNotNull("应该包含开发者统计", developerStats);
        assertTrue("开发者统计应该是数组", developerStats.isArray());

        // 从数据库获取实际数据进行对比
        List<Commit> dbCommits = commitRepository.findByProjectIdAndTimestampBetween(
                projectId, testStartTime, testEndTime);

        // 验证开发者数量一致性
        long uniqueDevelopers = dbCommits.stream()
                .map(Commit::getDeveloperId)
                .distinct()
                .count();
        assertEquals("开发者数量应该一致", uniqueDevelopers, developerStats.size());

        // 验证总体统计数据
        JsonNode totalStats = responseJson.get("totalStats");
        if (totalStats != null) {
            int apiTotalCommits = totalStats.get("totalCommits").asInt();
            int dbTotalCommits = dbCommits.size();
            assertEquals("总提交数应该一致", dbTotalCommits, apiTotalCommits);

            int apiTotalLinesAdded = totalStats.get("totalLinesAdded").asInt();
            int dbTotalLinesAdded = dbCommits.stream()
                    .mapToInt(c -> c.getLinesAdded() != null ? c.getLinesAdded() : 0)
                    .sum();
            assertEquals("总新增行数应该一致", dbTotalLinesAdded, apiTotalLinesAdded);
        }
    }

    @Test
    public void testQualityMetricsAPIDataConsistency() throws Exception {
        // When - 调用质量指标API
        String apiUrl = String.format("%s/api/v1/metrics/quality?projectId=%s&timeRange=7d",
                baseUrl, projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertEquals(projectId, responseJson.get("projectId").asText());

        // 验证质量指标数据一致性
        JsonNode qualityMetrics = responseJson.get("qualityMetrics");
        assertNotNull("应该包含质量指标", qualityMetrics);
        assertTrue("质量指标应该是数组", qualityMetrics.isArray());

        // 从数据库获取实际数据进行对比
        List<QualityMetrics> dbQualityMetrics = qualityMetricsRepository
                .findByProjectIdAndTimestampBetween(projectId, testStartTime, testEndTime);

        assertEquals("质量指标记录数应该一致", dbQualityMetrics.size(), qualityMetrics.size());

        // 验证概览数据
        JsonNode overview = responseJson.get("overview");
        if (overview != null) {
            int apiTotalScans = overview.get("totalScans").asInt();
            assertEquals("总扫描数应该一致", dbQualityMetrics.size(), apiTotalScans);

            int apiTotalBugs = overview.get("totalBugs").asInt();
            int dbTotalBugs = dbQualityMetrics.stream()
                    .mapToInt(q -> q.getBugs() != null ? q.getBugs() : 0)
                    .sum();
            assertEquals("总Bug数应该一致", dbTotalBugs, apiTotalBugs);
        }
    }

    @Test
    public void testTestCoverageAPIDataConsistency() throws Exception {
        // When - 调用测试覆盖率API
        String apiUrl = String.format("%s/api/v1/metrics/coverage?projectId=%s&timeRange=7d",
                baseUrl, projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertEquals(projectId, responseJson.get("projectId").asText());

        // 验证覆盖率数据一致性
        JsonNode coverageRecords = responseJson.get("coverageRecords");
        assertNotNull("应该包含覆盖率记录", coverageRecords);
        assertTrue("覆盖率记录应该是数组", coverageRecords.isArray());

        // 从数据库获取实际数据进行对比
        List<TestCoverage> dbCoverageRecords = testCoverageRepository
                .findByProjectIdAndTimestampBetween(projectId, testStartTime, testEndTime);

        assertEquals("覆盖率记录数应该一致", dbCoverageRecords.size(), coverageRecords.size());

        // 验证概览数据
        JsonNode overview = responseJson.get("overview");
        if (overview != null) {
            int apiTotalReports = overview.get("totalReports").asInt();
            assertEquals("总报告数应该一致", dbCoverageRecords.size(), apiTotalReports);

            if (overview.has("averageLineCoverage") && !dbCoverageRecords.isEmpty()) {
                double apiAvgLineCoverage = overview.get("averageLineCoverage").asDouble();
                double dbAvgLineCoverage = dbCoverageRecords.stream()
                        .filter(c -> c.getLineCoverage() != null)
                        .mapToDouble(TestCoverage::getLineCoverage)
                        .average()
                        .orElse(0.0);
                assertEquals("平均行覆盖率应该一致", dbAvgLineCoverage, apiAvgLineCoverage, 0.1);
            }
        }
    }

    @Test
    public void testDashboardAPIDataConsistency() throws Exception {
        // When - 调用效率看板API
        String apiUrl = String.format("%s/api/v1/metrics/dashboard?projectId=%s&timeRange=7d",
                baseUrl, projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertEquals(projectId, responseJson.get("projectId").asText());
        assertEquals("7d", responseJson.get("timeRange").asText());

        // 验证总体指标
        JsonNode overallMetrics = responseJson.get("overallMetrics");
        if (overallMetrics != null) {
            // 验证提交数据
            List<Commit> dbCommits = commitRepository.findByProjectIdAndTimestampBetween(
                    projectId, testStartTime, testEndTime);

            if (overallMetrics.has("totalCommits")) {
                int apiTotalCommits = overallMetrics.get("totalCommits").asInt();
                assertEquals("总提交数应该一致", dbCommits.size(), apiTotalCommits);
            }

            // 验证开发者数量
            if (overallMetrics.has("totalDevelopers")) {
                int apiTotalDevelopers = overallMetrics.get("totalDevelopers").asInt();
                long dbTotalDevelopers = dbCommits.stream()
                        .map(Commit::getDeveloperId)
                        .distinct()
                        .count();
                assertEquals("总开发者数应该一致", dbTotalDevelopers, apiTotalDevelopers);
            }

            // 验证合并请求数量
            if (overallMetrics.has("totalMergeRequests")) {
                int apiTotalMRs = overallMetrics.get("totalMergeRequests").asInt();
                List<MergeRequest> dbMRs = mergeRequestRepository
                        .findByProjectIdAndCreatedAtBetween(projectId, testStartTime, testEndTime);
                assertEquals("总合并请求数应该一致", dbMRs.size(), apiTotalMRs);
            }

            // 验证Issue数量
            if (overallMetrics.has("totalIssues")) {
                int apiTotalIssues = overallMetrics.get("totalIssues").asInt();
                List<Issue> dbIssues = issueRepository
                        .findByProjectIdAndCreatedAtBetween(projectId, testStartTime, testEndTime);
                assertEquals("总Issue数应该一致", dbIssues.size(), apiTotalIssues);
            }
        }
    }

    @Test
    public void testProjectOverviewAPIDataConsistency() throws Exception {
        // When - 调用项目概览API
        String apiUrl = String.format("%s/api/v1/metrics/overview?projectId=%s&timeRange=7d",
                baseUrl, projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertEquals(projectId, responseJson.get("projectId").asText());
        assertEquals("7d", responseJson.get("timeRange").asText());

        // 验证提交统计
        JsonNode commits = responseJson.get("commits");
        if (commits != null) {
            List<Commit> dbCommits = commitRepository.findByProjectIdAndTimestampBetween(
                    projectId, testStartTime, testEndTime);

            if (commits.has("totalCommits")) {
                int apiTotalCommits = commits.get("totalCommits").asInt();
                assertEquals("总提交数应该一致", dbCommits.size(), apiTotalCommits);
            }
        }

        // 验证最新质量指标
        JsonNode latestQuality = responseJson.get("latestQuality");
        if (latestQuality != null) {
            List<QualityMetrics> dbLatestQuality = qualityMetricsRepository.findLatestByProject(projectId);
            if (!dbLatestQuality.isEmpty()) {
                QualityMetrics dbLatest = dbLatestQuality.get(0);
                assertEquals("最新质量指标的提交SHA应该一致", 
                        dbLatest.getCommitSha(), latestQuality.get("commitSha").asText());
            }
        }

        // 验证最新覆盖率
        JsonNode latestCoverage = responseJson.get("latestCoverage");
        if (latestCoverage != null) {
            java.util.Optional<TestCoverage> dbLatestCoverage = testCoverageRepository
                    .findLatestByProject(projectId);
            if (dbLatestCoverage.isPresent()) {
                TestCoverage dbLatest = dbLatestCoverage.get();
                assertEquals("最新覆盖率的提交SHA应该一致", 
                        dbLatest.getCommitSha(), latestCoverage.get("commitSha").asText());
            }
        }
    }

    @Test
    public void testMultiProjectComparisonDataConsistency() throws Exception {
        // Given - 创建第二个项目的数据
        String projectId2 = "test-project-2";
        setupTestDataForProject(projectId2);

        // When - 调用多项目对比API
        String apiUrl = String.format("%s/api/v1/metrics/compare?projectIds=%s,%s&timeRange=7d",
                baseUrl, projectId, projectId2);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证项目数据
        JsonNode projects = responseJson.get("projects");
        assertNotNull("应该包含项目数据", projects);
        assertTrue("项目数据应该是数组", projects.isArray());
        assertEquals("应该有2个项目", 2, projects.size());

        // 验证每个项目的数据一致性
        for (JsonNode project : projects) {
            String currentProjectId = project.get("projectId").asText();
            assertTrue("项目ID应该是预期的", 
                    currentProjectId.equals(projectId) || currentProjectId.equals(projectId2));

            // 验证提交数据
            if (project.has("commits")) {
                int apiCommits = project.get("commits").asInt();
                List<Commit> dbCommits = commitRepository.findByProjectIdAndTimestampBetween(
                        currentProjectId, testStartTime, testEndTime);
                assertEquals("项目 " + currentProjectId + " 的提交数应该一致", 
                        dbCommits.size(), apiCommits);
            }

            // 验证开发者数量
            if (project.has("developers")) {
                int apiDevelopers = project.get("developers").asInt();
                List<Commit> dbCommits = commitRepository.findByProjectIdAndTimestampBetween(
                        currentProjectId, testStartTime, testEndTime);
                long dbDevelopers = dbCommits.stream()
                        .map(Commit::getDeveloperId)
                        .distinct()
                        .count();
                assertEquals("项目 " + currentProjectId + " 的开发者数应该一致", 
                        dbDevelopers, apiDevelopers);
            }
        }
    }

    @Test
    public void testRealtimeStatsDataConsistency() throws Exception {
        // When - 调用实时统计API
        String apiUrl = String.format("%s/api/v1/metrics/realtime", baseUrl);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应状态
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        // 验证基本字段
        assertNotNull("应该包含时间戳", responseJson.get("timestamp"));
        assertNotNull("应该包含日期", responseJson.get("date"));

        // 验证今日统计数据
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        if (responseJson.has("todayCommits")) {
            int apiTodayCommits = responseJson.get("todayCommits").asInt();
            List<Commit> dbTodayCommits = commitRepository.findByTimestampBetween(todayStart, now);
            assertEquals("今日提交数应该一致", dbTodayCommits.size(), apiTodayCommits);
        }

        if (responseJson.has("activeDevelopers")) {
            int apiActiveDevelopers = responseJson.get("activeDevelopers").asInt();
            List<Commit> dbTodayCommits = commitRepository.findByTimestampBetween(todayStart, now);
            long dbActiveDevelopers = dbTodayCommits.stream()
                    .map(Commit::getDeveloperId)
                    .distinct()
                    .count();
            assertEquals("活跃开发者数应该一致", dbActiveDevelopers, apiActiveDevelopers);
        }
    }

    @Test
    public void testAPIResponseTimeConsistency() throws Exception {
        // Given - 记录开始时间
        long startTime = System.currentTimeMillis();

        // When - 调用API
        String apiUrl = String.format("%s/api/v1/metrics/commits?startDate=%s&endDate=%s&projectId=%s",
                baseUrl, testStartTime.toString(), testEndTime.toString(), projectId);

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        // Then - 验证响应时间
        long responseTime = System.currentTimeMillis() - startTime;
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue("API响应时间应该在合理范围内（< 5秒）", responseTime < 5000);

        // 验证响应数据的完整性
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertNotNull("响应应该包含项目ID", responseJson.get("projectId"));
        assertNotNull("响应应该包含开始日期", responseJson.get("startDate"));
        assertNotNull("响应应该包含结束日期", responseJson.get("endDate"));
    }

    // Helper methods

    private void cleanupTestData() {
        fileChangeRepository.deleteAll();
        codeReviewRepository.deleteAll();
        testCoverageRepository.deleteAll();
        qualityMetricsRepository.deleteAll();
        mergeRequestRepository.deleteAll();
        issueRepository.deleteAll();
        commitRepository.deleteAll();
    }

    private void setupTestData() {
        setupTestDataForProject(projectId);
    }

    private void setupTestDataForProject(String projectId) {
        // 创建提交数据
        for (int i = 1; i <= 5; i++) {
            Commit commit = new Commit();
            commit.setCommitSha("commit-" + projectId + "-" + i);
            commit.setProjectId(projectId);
            commit.setDeveloperId("dev" + (i % 2 + 1));
            commit.setDeveloperName("Developer " + (i % 2 + 1));
            commit.setTimestamp(testStartTime.plusDays(i));
            commit.setMessage("Commit " + i + " for " + projectId);
            commit.setBranch("main");
            commit.setLinesAdded(100 + i * 10);
            commit.setLinesDeleted(50 + i * 5);
            commit.setFilesChanged(i);
            commitRepository.save(commit);

            // 创建质量指标数据
            QualityMetrics quality = new QualityMetrics();
            quality.setProjectId(projectId);
            quality.setCommitSha("commit-" + projectId + "-" + i);
            quality.setTimestamp(testStartTime.plusDays(i));
            quality.setCodeComplexity(5.0 + i);
            quality.setDuplicateRate(3.0 + i * 0.5);
            quality.setMaintainabilityIndex(80.0 + i);
            quality.setTechnicalDebt(10.0 + i);
            quality.setBugs(i);
            quality.setVulnerabilities(i % 2);
            quality.setCodeSmells(5 + i);
            quality.setQualityGate(i % 2 == 0 ? "PASSED" : "FAILED");
            qualityMetricsRepository.save(quality);

            // 创建测试覆盖率数据
            TestCoverage coverage = new TestCoverage();
            coverage.setProjectId(projectId);
            coverage.setCommitSha("commit-" + projectId + "-" + i);
            coverage.setTimestamp(testStartTime.plusDays(i));
            coverage.setLineCoverage(80.0 + i);
            coverage.setBranchCoverage(75.0 + i);
            coverage.setFunctionCoverage(85.0 + i);
            coverage.setTotalLines(1000 + i * 100);
            coverage.setCoveredLines((int) ((1000 + i * 100) * (80.0 + i) / 100));
            coverage.setStatus(i % 2 == 0 ? "PASSED" : "FAILED");
            testCoverageRepository.save(coverage);
        }

        // 创建合并请求数据
        for (int i = 1; i <= 3; i++) {
            MergeRequest mr = new MergeRequest();
            mr.setMrId("mr-" + projectId + "-" + i);
            mr.setProjectId(projectId);
            mr.setAuthorId("author" + i);
            mr.setCreatedAt(testStartTime.plusDays(i));
            mr.setStatus("opened");
            mr.setSourceBranch("feature-" + i);
            mr.setTargetBranch("main");
            mr.setChangedFiles(i * 2);
            mr.setAdditions(100 + i * 20);
            mr.setDeletions(50 + i * 10);
            mergeRequestRepository.save(mr);
        }

        // 创建Issue数据
        for (int i = 1; i <= 4; i++) {
            Issue issue = new Issue();
            issue.setIssueId("issue-" + projectId + "-" + i);
            issue.setProjectId(projectId);
            issue.setTitle("Issue " + i + " for " + projectId);
            issue.setDescription("Description for issue " + i);
            issue.setAuthorId("author" + i);
            issue.setAuthorName("Author " + i);
            issue.setCreatedAt(testStartTime.plusDays(i));
            issue.setStatus(i % 2 == 0 ? "closed" : "opened");
            issue.setIssueType("bug");
            issue.setPriority("medium");
            issue.setSeverity("minor");
            issueRepository.save(issue);
        }
    }
}