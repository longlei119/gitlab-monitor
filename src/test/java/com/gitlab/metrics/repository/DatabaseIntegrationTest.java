package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * 数据库集成测试
 * 测试Repository层与数据库的集成
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class DatabaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

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

    private String projectId;
    private LocalDateTime testTime;

    @Before
    public void setUp() {
        projectId = "test-project";
        testTime = LocalDateTime.now();
    }

    @Test
    public void testCommitRepository_SaveAndFind() {
        // Given
        Commit commit = createTestCommit();

        // When
        Commit savedCommit = commitRepository.save(commit);
        entityManager.flush();

        // Then
        assertNotNull(savedCommit.getId());
        assertEquals(commit.getCommitSha(), savedCommit.getCommitSha());
        assertEquals(commit.getProjectId(), savedCommit.getProjectId());

        // Test find by commit SHA
        Optional<Commit> foundCommit = commitRepository.findByCommitSha(commit.getCommitSha());
        assertTrue(foundCommit.isPresent());
        assertEquals(commit.getCommitSha(), foundCommit.get().getCommitSha());
    }

    @Test
    public void testCommitRepository_FindByProjectAndDateRange() {
        // Given
        Commit commit1 = createTestCommit();
        commit1.setTimestamp(testTime.minusDays(1));
        Commit commit2 = createTestCommit();
        commit2.setCommitSha("commit2");
        commit2.setTimestamp(testTime.minusDays(2));
        Commit commit3 = createTestCommit();
        commit3.setCommitSha("commit3");
        commit3.setProjectId("other-project");
        commit3.setTimestamp(testTime.minusDays(1));

        commitRepository.save(commit1);
        commitRepository.save(commit2);
        commitRepository.save(commit3);
        entityManager.flush();

        // When
        List<Commit> commits = commitRepository.findByProjectIdAndTimestampBetween(
            projectId, testTime.minusDays(3), testTime);

        // Then
        assertEquals(2, commits.size());
        assertTrue(commits.stream().allMatch(c -> projectId.equals(c.getProjectId())));
    }

    @Test
    public void testCommitRepository_GetDeveloperStats() {
        // Given
        Commit commit1 = createTestCommit();
        commit1.setDeveloperId("dev1");
        commit1.setLinesAdded(100);
        commit1.setLinesDeleted(50);
        Commit commit2 = createTestCommit();
        commit2.setCommitSha("commit2");
        commit2.setDeveloperId("dev1");
        commit2.setLinesAdded(200);
        commit2.setLinesDeleted(100);
        Commit commit3 = createTestCommit();
        commit3.setCommitSha("commit3");
        commit3.setDeveloperId("dev2");
        commit3.setLinesAdded(150);
        commit3.setLinesDeleted(75);

        commitRepository.save(commit1);
        commitRepository.save(commit2);
        commitRepository.save(commit3);
        entityManager.flush();

        // When
        List<Object[]> stats = commitRepository.getDeveloperCommitStats(
            testTime.minusDays(1), testTime.plusDays(1), projectId, null);

        // Then
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        // 验证统计结果包含开发者信息
        boolean foundDev1 = false;
        boolean foundDev2 = false;
        for (Object[] stat : stats) {
            String developerId = (String) stat[0];
            if ("dev1".equals(developerId)) {
                foundDev1 = true;
                assertEquals(2L, stat[2]); // commit count
                assertEquals(300L, stat[3]); // lines added
                assertEquals(150L, stat[4]); // lines deleted
            } else if ("dev2".equals(developerId)) {
                foundDev2 = true;
                assertEquals(1L, stat[2]); // commit count
                assertEquals(150L, stat[3]); // lines added
                assertEquals(75L, stat[4]); // lines deleted
            }
        }
        assertTrue(foundDev1);
        assertTrue(foundDev2);
    }

    @Test
    public void testQualityMetricsRepository_SaveAndFind() {
        // Given
        QualityMetrics metrics = createTestQualityMetrics();

        // When
        QualityMetrics savedMetrics = qualityMetricsRepository.save(metrics);
        entityManager.flush();

        // Then
        assertNotNull(savedMetrics.getId());
        assertEquals(metrics.getProjectId(), savedMetrics.getProjectId());
        assertEquals(metrics.getCommitSha(), savedMetrics.getCommitSha());

        // Test find by project and date range
        List<QualityMetrics> found = qualityMetricsRepository.findByProjectIdAndTimestampBetween(
            projectId, testTime.minusHours(1), testTime.plusHours(1));
        assertEquals(1, found.size());
        assertEquals(metrics.getCommitSha(), found.get(0).getCommitSha());
    }

    @Test
    public void testQualityMetricsRepository_GetQualityTrend() {
        // Given
        QualityMetrics metrics1 = createTestQualityMetrics();
        metrics1.setCodeComplexity(5.0);
        metrics1.setDuplicateRate(3.0);
        QualityMetrics metrics2 = createTestQualityMetrics();
        metrics2.setCommitSha("commit2");
        metrics2.setCodeComplexity(4.0);
        metrics2.setDuplicateRate(2.5);

        qualityMetricsRepository.save(metrics1);
        qualityMetricsRepository.save(metrics2);
        entityManager.flush();

        // When
        Object[] trend = qualityMetricsRepository.getQualityTrend(
            projectId, testTime.minusHours(1), testTime.plusHours(1));

        // Then
        assertNotNull(trend);
        assertEquals(4, trend.length);
        assertEquals(4.5, (Double) trend[0], 0.1); // average complexity
        assertEquals(2.75, (Double) trend[1], 0.1); // average duplicate rate
    }

    @Test
    public void testTestCoverageRepository_SaveAndFind() {
        // Given
        TestCoverage coverage = createTestTestCoverage();

        // When
        TestCoverage savedCoverage = testCoverageRepository.save(coverage);
        entityManager.flush();

        // Then
        assertNotNull(savedCoverage.getId());
        assertEquals(coverage.getProjectId(), savedCoverage.getProjectId());
        assertEquals(coverage.getLineCoverage(), savedCoverage.getLineCoverage());

        // Test find latest by project
        Optional<TestCoverage> latest = testCoverageRepository.findLatestByProject(projectId);
        assertTrue(latest.isPresent());
        assertEquals(coverage.getCommitSha(), latest.get().getCommitSha());
    }

    @Test
    public void testMergeRequestRepository_SaveAndFind() {
        // Given
        MergeRequest mr = createTestMergeRequest();

        // When
        MergeRequest savedMr = mergeRequestRepository.save(mr);
        entityManager.flush();

        // Then
        assertNotNull(savedMr.getId());
        assertEquals(mr.getMrId(), savedMr.getMrId());
        assertEquals(mr.getProjectId(), savedMr.getProjectId());

        // Test find by MR ID
        Optional<MergeRequest> found = mergeRequestRepository.findByMrId(mr.getMrId());
        assertTrue(found.isPresent());
        assertEquals(mr.getMrId(), found.get().getMrId());
    }

    @Test
    public void testIssueRepository_SaveAndFind() {
        // Given
        Issue issue = createTestIssue();

        // When
        Issue savedIssue = issueRepository.save(issue);
        entityManager.flush();

        // Then
        assertNotNull(savedIssue.getId());
        assertEquals(issue.getIssueId(), savedIssue.getIssueId());
        assertEquals(issue.getProjectId(), savedIssue.getProjectId());

        // Test find by issue ID
        Optional<Issue> found = issueRepository.findByIssueId(issue.getIssueId());
        assertTrue(found.isPresent());
        assertEquals(issue.getIssueId(), found.get().getIssueId());
    }

    @Test
    public void testCodeReviewRepository_SaveAndFind() {
        // Given
        MergeRequest mr = createTestMergeRequest();
        mergeRequestRepository.save(mr);
        entityManager.flush();

        CodeReview review = createTestCodeReview(mr);

        // When
        CodeReview savedReview = codeReviewRepository.save(review);
        entityManager.flush();

        // Then
        assertNotNull(savedReview.getId());
        assertEquals(review.getReviewerId(), savedReview.getReviewerId());
        assertEquals(review.getMergeRequest().getId(), savedReview.getMergeRequest().getId());

        // Test find by merge request
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestId(mr.getId());
        assertEquals(1, reviews.size());
        assertEquals(review.getReviewerId(), reviews.get(0).getReviewerId());
    }

    @Test
    public void testFileChangeRepository_SaveAndFind() {
        // Given
        Commit commit = createTestCommit();
        commitRepository.save(commit);
        entityManager.flush();

        FileChange fileChange = createTestFileChange(commit);

        // When
        FileChange savedFileChange = fileChangeRepository.save(fileChange);
        entityManager.flush();

        // Then
        assertNotNull(savedFileChange.getId());
        assertEquals(fileChange.getFilePath(), savedFileChange.getFilePath());
        assertEquals(fileChange.getCommit().getId(), savedFileChange.getCommit().getId());

        // Test find by commit
        List<FileChange> changes = fileChangeRepository.findByCommitId(commit.getId());
        assertEquals(1, changes.size());
        assertEquals(fileChange.getFilePath(), changes.get(0).getFilePath());
    }

    @Test
    public void testTransactionalBehavior() {
        // Given
        Commit commit = createTestCommit();
        QualityMetrics metrics = createTestQualityMetrics();

        // When - 保存相关实体
        Commit savedCommit = commitRepository.save(commit);
        metrics.setCommitSha(savedCommit.getCommitSha());
        QualityMetrics savedMetrics = qualityMetricsRepository.save(metrics);
        entityManager.flush();

        // Then - 验证关联关系
        assertNotNull(savedCommit.getId());
        assertNotNull(savedMetrics.getId());
        assertEquals(savedCommit.getCommitSha(), savedMetrics.getCommitSha());

        // 验证可以通过commit SHA查找相关的质量指标
        List<QualityMetrics> relatedMetrics = qualityMetricsRepository.findByCommitSha(savedCommit.getCommitSha());
        assertEquals(1, relatedMetrics.size());
        assertEquals(savedMetrics.getId(), relatedMetrics.get(0).getId());
    }

    @Test
    public void testCascadeOperations() {
        // Given
        MergeRequest mr = createTestMergeRequest();
        CodeReview review1 = createTestCodeReview(mr);
        CodeReview review2 = createTestCodeReview(mr);
        review2.setReviewerId("reviewer2");
        review2.setReviewerName("Reviewer 2");

        mr.setReviews(java.util.Arrays.asList(review1, review2));

        // When
        MergeRequest savedMr = mergeRequestRepository.save(mr);
        entityManager.flush();

        // Then
        assertNotNull(savedMr.getId());
        assertEquals(2, savedMr.getReviews().size());

        // 验证级联保存
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestId(savedMr.getId());
        assertEquals(2, reviews.size());
    }

    @Test
    public void testComplexQueries() {
        // Given - 创建测试数据
        setupComplexTestData();

        // When & Then - 测试复杂查询
        // 测试按时间范围查询提交统计
        List<Object[]> commitStats = commitRepository.getDeveloperCommitStats(
            testTime.minusDays(7), testTime.plusDays(1), projectId, null);
        assertNotNull(commitStats);
        assertFalse(commitStats.isEmpty());

        // 测试质量趋势查询
        Object[] qualityTrend = qualityMetricsRepository.getQualityTrend(
            projectId, testTime.minusDays(7), testTime.plusDays(1));
        assertNotNull(qualityTrend);

        // 测试覆盖率统计查询
        List<Object[]> coverageStats = testCoverageRepository.getCoverageStatusStats(
            projectId, testTime.minusDays(7), testTime.plusDays(1));
        assertNotNull(coverageStats);
    }

    // Helper methods to create test entities

    private Commit createTestCommit() {
        Commit commit = new Commit();
        commit.setCommitSha("test-commit-sha");
        commit.setProjectId(projectId);
        commit.setDeveloperId("test-developer");
        commit.setDeveloperName("Test Developer");
        commit.setTimestamp(testTime);
        commit.setMessage("Test commit message");
        commit.setBranch("main");
        commit.setLinesAdded(100);
        commit.setLinesDeleted(50);
        commit.setFilesChanged(5);
        return commit;
    }

    private QualityMetrics createTestQualityMetrics() {
        QualityMetrics metrics = new QualityMetrics();
        metrics.setProjectId(projectId);
        metrics.setCommitSha("test-commit-sha");
        metrics.setTimestamp(testTime);
        metrics.setCodeComplexity(5.0);
        metrics.setDuplicateRate(3.0);
        metrics.setMaintainabilityIndex(80.0);
        metrics.setTechnicalDebt(10.0);
        metrics.setBugs(2);
        metrics.setVulnerabilities(1);
        metrics.setCodeSmells(5);
        metrics.setQualityGate("PASSED");
        return metrics;
    }

    private TestCoverage createTestTestCoverage() {
        TestCoverage coverage = new TestCoverage();
        coverage.setProjectId(projectId);
        coverage.setCommitSha("test-commit-sha");
        coverage.setTimestamp(testTime);
        coverage.setLineCoverage(85.0);
        coverage.setBranchCoverage(80.0);
        coverage.setFunctionCoverage(90.0);
        coverage.setTotalLines(1000);
        coverage.setCoveredLines(850);
        coverage.setTotalBranches(200);
        coverage.setCoveredBranches(160);
        coverage.setStatus("PASSED");
        return coverage;
    }

    private MergeRequest createTestMergeRequest() {
        MergeRequest mr = new MergeRequest();
        mr.setMrId("123");
        mr.setProjectId(projectId);
        mr.setAuthorId("author1");
        mr.setCreatedAt(testTime);
        mr.setStatus("opened");
        mr.setSourceBranch("feature-branch");
        mr.setTargetBranch("main");
        mr.setChangedFiles(5);
        mr.setAdditions(100);
        mr.setDeletions(50);
        return mr;
    }

    private Issue createTestIssue() {
        Issue issue = new Issue();
        issue.setIssueId("456");
        issue.setProjectId(projectId);
        issue.setTitle("Test Issue");
        issue.setDescription("Test issue description");
        issue.setAuthorId("author1");
        issue.setAuthorName("Author 1");
        issue.setCreatedAt(testTime);
        issue.setStatus("opened");
        issue.setIssueType("bug");
        issue.setPriority("medium");
        issue.setSeverity("minor");
        return issue;
    }

    private CodeReview createTestCodeReview(MergeRequest mr) {
        CodeReview review = new CodeReview();
        review.setMergeRequest(mr);
        review.setReviewerId("reviewer1");
        review.setReviewerName("Reviewer 1");
        review.setReviewedAt(testTime);
        review.setStatus("approved");
        review.setComment("Looks good to me");
        return review;
    }

    private FileChange createTestFileChange(Commit commit) {
        FileChange fileChange = new FileChange();
        fileChange.setCommit(commit);
        fileChange.setFilePath("src/main/java/TestFile.java");
        fileChange.setChangeType("modified");
        fileChange.setLinesAdded(20);
        fileChange.setLinesDeleted(10);
        return fileChange;
    }

    private void setupComplexTestData() {
        // 创建多个提交
        for (int i = 1; i <= 5; i++) {
            Commit commit = createTestCommit();
            commit.setCommitSha("commit-" + i);
            commit.setTimestamp(testTime.minusDays(i));
            commit.setDeveloperId("dev" + (i % 2 + 1));
            commitRepository.save(commit);

            // 为每个提交创建质量指标
            QualityMetrics metrics = createTestQualityMetrics();
            metrics.setCommitSha("commit-" + i);
            metrics.setTimestamp(testTime.minusDays(i));
            metrics.setCodeComplexity(5.0 + i);
            qualityMetricsRepository.save(metrics);

            // 为每个提交创建测试覆盖率
            TestCoverage coverage = createTestTestCoverage();
            coverage.setCommitSha("commit-" + i);
            coverage.setTimestamp(testTime.minusDays(i));
            coverage.setLineCoverage(80.0 + i);
            testCoverageRepository.save(coverage);
        }

        entityManager.flush();
    }
}