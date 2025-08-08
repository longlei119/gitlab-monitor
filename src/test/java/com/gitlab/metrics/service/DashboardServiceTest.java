package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.DashboardResponse;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceTest {

    @Mock
    private CommitRepository commitRepository;

    @Mock
    private QualityMetricsRepository qualityMetricsRepository;

    @Mock
    private TestCoverageRepository testCoverageRepository;

    @Mock
    private MergeRequestRepository mergeRequestRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CommitStatisticsService commitStatisticsService;

    @Mock
    private BugFixEfficiencyService bugFixEfficiencyService;

    @InjectMocks
    private DashboardService dashboardService;

    private String projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timeRange;

    @Before
    public void setUp() {
        projectId = "test-project";
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
        timeRange = "30d";
    }

    @Test
    public void testGetDashboardData_Success() {
        // Given
        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        assertNotNull(response);
        assertEquals(projectId, response.getProjectId());
        assertEquals(timeRange, response.getTimeRange());
        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertNotNull(response.getOverallMetrics());
        assertNotNull(response.getDeveloperEfficiency());
        assertNotNull(response.getQualityTrends());
        assertNotNull(response.getProductivityMetrics());
        assertNotNull(response.getTrendData());
    }

    @Test
    public void testGetDashboardData_WithException() {
        // Given
        when(commitStatisticsService.getProjectTotalStats(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("获取效率看板数据失败", e.getMessage());
        }
    }

    @Test
    public void testBuildOverallMetrics() {
        // Given
        CommitStatisticsService.ProjectTotalStats commitStats = createMockProjectTotalStats();
        when(commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate))
            .thenReturn(commitStats);

        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        when(commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null))
            .thenReturn(developerStats);

        when(mergeRequestRepository.findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate))
            .thenReturn(createMockMergeRequests());

        when(issueRepository.findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate))
            .thenReturn(createMockIssues());

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        DashboardResponse.OverallMetrics metrics = response.getOverallMetrics();
        assertNotNull(metrics);
        assertEquals(100, metrics.getTotalCommits());
        assertEquals(1000, metrics.getTotalLinesAdded());
        assertEquals(500, metrics.getTotalLinesDeleted());
        assertEquals(15.0, metrics.getAverageCommitSize(), 0.1);
        assertEquals(50.0, metrics.getCodeChurnRate(), 0.1);
        assertEquals(2, metrics.getTotalDevelopers());
        assertEquals(5, metrics.getTotalMergeRequests());
        assertEquals(3, metrics.getTotalIssues());
    }

    @Test
    public void testBuildDeveloperEfficiency() {
        // Given
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        when(commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null))
            .thenReturn(developerStats);

        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        DashboardResponse.DeveloperEfficiency efficiency = response.getDeveloperEfficiency();
        assertNotNull(efficiency);
        assertNotNull(efficiency.getTopDevelopers());
        assertTrue(efficiency.getTopDevelopers().size() <= 10);
        assertTrue(efficiency.getAverageCommitsPerDeveloper() > 0);
        assertTrue(efficiency.getAverageLinesPerDeveloper() > 0);
        assertNotNull(efficiency.getDeveloperDistribution());
    }

    @Test
    public void testBuildQualityTrends() {
        // Given
        List<QualityMetrics> currentMetrics = createMockQualityMetrics();
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(currentMetrics);

        List<Issue> bugs = createMockBugs();
        when(issueRepository.findBugsByProjectAndDateRange(projectId, startDate, endDate))
            .thenReturn(bugs);

        List<TestCoverage> coverageRecords = createMockTestCoverage();
        when(testCoverageRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(coverageRecords);

        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        DashboardResponse.QualityTrends trends = response.getQualityTrends();
        assertNotNull(trends);
        assertTrue(trends.getCurrentQualityScore() >= 0);
        assertEquals(3, trends.getTotalBugs());
        assertEquals(2, trends.getFixedBugs());
        assertEquals(66.67, trends.getBugFixRate(), 0.1);
        assertTrue(trends.getTestCoverage() >= 0);
    }

    @Test
    public void testBuildProductivityMetrics() {
        // Given
        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        DashboardResponse.ProductivityMetrics metrics = response.getProductivityMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getCommitsPerDay() >= 0);
        assertTrue(metrics.getLinesPerDay() >= 0);
        assertTrue(metrics.getMergeRequestsPerDay() >= 0);
        assertTrue(metrics.getActiveContributors() >= 0);
    }

    @Test
    public void testBuildTrendData() {
        // Given
        List<CommitStatisticsService.CommitTrendData> commitTrend = createMockCommitTrendData();
        when(commitStatisticsService.getCommitTrend(startDate, endDate, projectId))
            .thenReturn(commitTrend);

        when(qualityMetricsRepository.getQualityTrendByDate(projectId, startDate, endDate))
            .thenReturn(createMockQualityTrendData());

        when(testCoverageRepository.getCoverageTrendByDate(projectId, startDate, endDate))
            .thenReturn(createMockCoverageTrendData());

        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        List<DashboardResponse.TrendPoint> trendData = response.getTrendData();
        assertNotNull(trendData);
        assertFalse(trendData.isEmpty());
        
        DashboardResponse.TrendPoint firstPoint = trendData.get(0);
        assertNotNull(firstPoint.getDate());
        assertTrue(firstPoint.getCommits() >= 0);
        assertTrue(firstPoint.getLinesAdded() >= 0);
        assertTrue(firstPoint.getLinesDeleted() >= 0);
    }

    @Test
    public void testCalculateDeveloperEfficiency() {
        // Given
        CommitStatisticsService.DeveloperCommitStats stats = new CommitStatisticsService.DeveloperCommitStats();
        stats.setDeveloperId("dev1");
        stats.setDeveloperName("Developer 1");
        stats.setCommitCount(20);
        stats.setLinesAdded(500);
        stats.setLinesDeleted(100);
        stats.setFilesChanged(50);

        List<CommitStatisticsService.DeveloperCommitStats> developerStats = Arrays.asList(stats);
        when(commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null))
            .thenReturn(developerStats);

        setupMockData();

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        DashboardResponse.DeveloperEfficiency efficiency = response.getDeveloperEfficiency();
        assertNotNull(efficiency.getTopDevelopers());
        assertFalse(efficiency.getTopDevelopers().isEmpty());
        
        DashboardResponse.DeveloperMetric metric = efficiency.getTopDevelopers().get(0);
        assertEquals("dev1", metric.getDeveloperId());
        assertEquals("Developer 1", metric.getDeveloperName());
        assertEquals(20, metric.getCommits());
        assertEquals(500, metric.getLinesAdded());
        assertEquals(100, metric.getLinesDeleted());
        assertEquals(50, metric.getFilesChanged());
        assertTrue(metric.getEfficiency() > 0);
    }

    @Test
    public void testEmptyDataHandling() {
        // Given - 所有数据都为空
        when(commitStatisticsService.getProjectTotalStats(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(createEmptyProjectTotalStats());
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), anyString(), any()))
            .thenReturn(Collections.emptyList());
        when(mergeRequestRepository.findByProjectIdAndCreatedAtBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(issueRepository.findByProjectIdAndCreatedAtBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(testCoverageRepository.findByProjectIdAndTimestampBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(issueRepository.findBugsByProjectAndDateRange(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(commitStatisticsService.getCommitTrend(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
            .thenReturn(Collections.emptyList());
        when(qualityMetricsRepository.getQualityTrendByDate(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(testCoverageRepository.getCoverageTrendByDate(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(projectId, startDate, endDate, timeRange);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getOverallMetrics().getTotalCommits());
        assertEquals(0.0, response.getOverallMetrics().getAverageCommitSize(), 0.1);
        assertEquals(0, response.getDeveloperEfficiency().getTopDevelopers().size());
        assertEquals(0, response.getQualityTrends().getTotalBugs());
        assertTrue(response.getTrendData().isEmpty());
    }

    // Helper methods to create mock data

    private void setupMockData() {
        when(commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate))
            .thenReturn(createMockProjectTotalStats());
        when(commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null))
            .thenReturn(createMockDeveloperStats());
        when(mergeRequestRepository.findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate))
            .thenReturn(createMockMergeRequests());
        when(issueRepository.findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate))
            .thenReturn(createMockIssues());
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(createMockQualityMetrics());
        when(testCoverageRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(createMockTestCoverage());
        when(issueRepository.findBugsByProjectAndDateRange(projectId, startDate, endDate))
            .thenReturn(createMockBugs());
        when(commitStatisticsService.getCommitTrend(startDate, endDate, projectId))
            .thenReturn(createMockCommitTrendData());
        when(qualityMetricsRepository.getQualityTrendByDate(projectId, startDate, endDate))
            .thenReturn(createMockQualityTrendData());
        when(testCoverageRepository.getCoverageTrendByDate(projectId, startDate, endDate))
            .thenReturn(createMockCoverageTrendData());
    }

    private CommitStatisticsService.ProjectTotalStats createMockProjectTotalStats() {
        CommitStatisticsService.ProjectTotalStats stats = new CommitStatisticsService.ProjectTotalStats();
        stats.setTotalCommits(100);
        stats.setTotalLinesAdded(1000);
        stats.setTotalLinesDeleted(500);
        return stats;
    }

    private CommitStatisticsService.ProjectTotalStats createEmptyProjectTotalStats() {
        CommitStatisticsService.ProjectTotalStats stats = new CommitStatisticsService.ProjectTotalStats();
        stats.setTotalCommits(0);
        stats.setTotalLinesAdded(0);
        stats.setTotalLinesDeleted(0);
        return stats;
    }

    private List<CommitStatisticsService.DeveloperCommitStats> createMockDeveloperStats() {
        CommitStatisticsService.DeveloperCommitStats dev1 = new CommitStatisticsService.DeveloperCommitStats();
        dev1.setDeveloperId("dev1");
        dev1.setDeveloperName("Developer 1");
        dev1.setCommitCount(60);
        dev1.setLinesAdded(600);
        dev1.setLinesDeleted(300);
        dev1.setFilesChanged(30);

        CommitStatisticsService.DeveloperCommitStats dev2 = new CommitStatisticsService.DeveloperCommitStats();
        dev2.setDeveloperId("dev2");
        dev2.setDeveloperName("Developer 2");
        dev2.setCommitCount(40);
        dev2.setLinesAdded(400);
        dev2.setLinesDeleted(200);
        dev2.setFilesChanged(20);

        return Arrays.asList(dev1, dev2);
    }

    private List<MergeRequest> createMockMergeRequests() {
        MergeRequest mr1 = new MergeRequest();
        mr1.setMrId("1");
        mr1.setProjectId(projectId);
        mr1.setCreatedAt(startDate.plusDays(1));
        mr1.setMergedAt(startDate.plusDays(2));

        MergeRequest mr2 = new MergeRequest();
        mr2.setMrId("2");
        mr2.setProjectId(projectId);
        mr2.setCreatedAt(startDate.plusDays(5));
        mr2.setMergedAt(startDate.plusDays(6));

        return Arrays.asList(mr1, mr2, new MergeRequest(), new MergeRequest(), new MergeRequest());
    }

    private List<Issue> createMockIssues() {
        Issue issue1 = new Issue();
        issue1.setIssueId("1");
        issue1.setProjectId(projectId);
        issue1.setCreatedAt(startDate.plusDays(1));

        Issue issue2 = new Issue();
        issue2.setIssueId("2");
        issue2.setProjectId(projectId);
        issue2.setCreatedAt(startDate.plusDays(2));

        return Arrays.asList(issue1, issue2, new Issue());
    }

    private List<QualityMetrics> createMockQualityMetrics() {
        QualityMetrics metrics1 = new QualityMetrics();
        metrics1.setProjectId(projectId);
        metrics1.setBugs(2);
        metrics1.setVulnerabilities(1);
        metrics1.setCodeSmells(10);
        metrics1.setDuplicateRate(5.0);

        QualityMetrics metrics2 = new QualityMetrics();
        metrics2.setProjectId(projectId);
        metrics2.setBugs(1);
        metrics2.setVulnerabilities(0);
        metrics2.setCodeSmells(8);
        metrics2.setDuplicateRate(3.0);

        return Arrays.asList(metrics1, metrics2);
    }

    private List<TestCoverage> createMockTestCoverage() {
        TestCoverage coverage1 = new TestCoverage();
        coverage1.setProjectId(projectId);
        coverage1.setLineCoverage(85.0);

        TestCoverage coverage2 = new TestCoverage();
        coverage2.setProjectId(projectId);
        coverage2.setLineCoverage(90.0);

        return Arrays.asList(coverage1, coverage2);
    }

    private List<Issue> createMockBugs() {
        Issue bug1 = new Issue();
        bug1.setIssueId("bug1");
        bug1.setProjectId(projectId);
        bug1.setState("closed");
        bug1.setCreatedAt(startDate.plusDays(1));
        bug1.setClosedAt(startDate.plusDays(3));

        Issue bug2 = new Issue();
        bug2.setIssueId("bug2");
        bug2.setProjectId(projectId);
        bug2.setState("closed");
        bug2.setCreatedAt(startDate.plusDays(2));
        bug2.setClosedAt(startDate.plusDays(4));

        Issue bug3 = new Issue();
        bug3.setIssueId("bug3");
        bug3.setProjectId(projectId);
        bug3.setState("open");
        bug3.setCreatedAt(startDate.plusDays(5));

        return Arrays.asList(bug1, bug2, bug3);
    }

    private List<CommitStatisticsService.CommitTrendData> createMockCommitTrendData() {
        CommitStatisticsService.CommitTrendData trend1 = new CommitStatisticsService.CommitTrendData();
        trend1.setDate(LocalDate.now().minusDays(2));
        trend1.setCommitCount(10);
        trend1.setLinesAdded(100);
        trend1.setLinesDeleted(50);

        CommitStatisticsService.CommitTrendData trend2 = new CommitStatisticsService.CommitTrendData();
        trend2.setDate(LocalDate.now().minusDays(1));
        trend2.setCommitCount(15);
        trend2.setLinesAdded(150);
        trend2.setLinesDeleted(75);

        return Arrays.asList(trend1, trend2);
    }

    private List<Object[]> createMockQualityTrendData() {
        return Arrays.asList(
            new Object[]{LocalDate.now().minusDays(2).toString(), 5.0, 3.0, 80.0, 10.0},
            new Object[]{LocalDate.now().minusDays(1).toString(), 4.0, 2.0, 85.0, 8.0}
        );
    }

    private List<Object[]> createMockCoverageTrendData() {
        return Arrays.asList(
            new Object[]{LocalDate.now().minusDays(2).toString(), 85.0},
            new Object[]{LocalDate.now().minusDays(1).toString(), 90.0}
        );
    }
}