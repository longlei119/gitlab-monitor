package com.gitlab.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import com.gitlab.metrics.repository.TestCoverageRepository;
import com.gitlab.metrics.service.CommitStatisticsService;
import com.gitlab.metrics.service.DashboardService;
import com.gitlab.metrics.service.TestCoverageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MetricsController集成测试
 */
@RunWith(SpringRunner.class)
@WebMvcTest(MetricsController.class)
public class MetricsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommitStatisticsService commitStatisticsService;

    @MockBean
    private QualityMetricsRepository qualityMetricsRepository;

    @MockBean
    private TestCoverageRepository testCoverageRepository;

    @MockBean
    private TestCoverageService testCoverageService;

    @MockBean
    private DashboardService dashboardService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String projectId;

    @Before
    public void setUp() {
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
        projectId = "test-project";
    }

    @Test
    public void testGetCommitStats_Success() throws Exception {
        // Given
        CommitStatisticsService.ProjectTotalStats totalStats = createMockProjectTotalStats();
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        List<CommitStatisticsService.CommitTrendData> trendData = createMockTrendData();

        when(commitStatisticsService.getProjectTotalStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(totalStats);
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(projectId), isNull()))
            .thenReturn(developerStats);
        when(commitStatisticsService.getCommitTrend(any(LocalDateTime.class), any(LocalDateTime.class), eq(projectId)))
            .thenReturn(trendData);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/commits")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("projectId", projectId)
                .param("includeDetails", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.developerStats").isArray())
                .andExpect(jsonPath("$.trendData").isArray())
                .andExpect(jsonPath("$.totalStats").exists());

        verify(commitStatisticsService).getProjectTotalStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(commitStatisticsService).getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(projectId), isNull());
        verify(commitStatisticsService).getCommitTrend(any(LocalDateTime.class), any(LocalDateTime.class), eq(projectId));
    }

    @Test
    public void testGetCommitStats_WithoutProjectId() throws Exception {
        // Given
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        List<CommitStatisticsService.ProjectCommitStats> projectStats = createMockProjectStats();
        List<CommitStatisticsService.CommitTrendData> trendData = createMockTrendData();

        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
            .thenReturn(developerStats);
        when(commitStatisticsService.getProjectCommitStats(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(projectStats);
        when(commitStatisticsService.getCommitTrend(any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
            .thenReturn(trendData);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/commits")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("includeDetails", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").isEmpty())
                .andExpect(jsonPath("$.developerStats").isArray())
                .andExpect(jsonPath("$.projectStats").isArray())
                .andExpect(jsonPath("$.trendData").isArray());

        verify(commitStatisticsService).getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull());
        verify(commitStatisticsService).getProjectCommitStats(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(commitStatisticsService).getCommitTrend(any(LocalDateTime.class), any(LocalDateTime.class), isNull());
    }

    @Test
    public void testGetCommitStats_MinimalDetails() throws Exception {
        // Given
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/commits")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("includeDetails", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.developerStats").doesNotExist())
                .andExpect(jsonPath("$.projectStats").doesNotExist())
                .andExpect(jsonPath("$.trendData").doesNotExist());

        verify(commitStatisticsService, never()).getDeveloperCommitStats(any(), any(), any(), any());
    }

    @Test
    public void testGetQualityMetrics_Success() throws Exception {
        // Given
        List<QualityMetrics> qualityMetrics = createMockQualityMetrics();
        Object[] trendResult = {5.0, 3.0, 80.0, 10.0};
        List<Object[]> trendByDate = createMockQualityTrendByDate();
        List<Object[]> qualityGateStats = createMockQualityGateStats();

        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(qualityMetrics);
        when(qualityMetricsRepository.getQualityTrend(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(trendResult);
        when(qualityMetricsRepository.getQualityTrendByDate(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(trendByDate);
        when(qualityMetricsRepository.getQualityGateStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(qualityGateStats);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/quality")
                .param("projectId", projectId)
                .param("timeRange", "30d")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.timeRange").value("30d"))
                .andExpect(jsonPath("$.qualityMetrics").isArray())
                .andExpect(jsonPath("$.trendData").exists())
                .andExpect(jsonPath("$.overview").exists());

        verify(qualityMetricsRepository).findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(qualityMetricsRepository).getQualityTrend(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    public void testGetQualityMetrics_CustomDateRange() throws Exception {
        // Given
        LocalDateTime customStart = LocalDateTime.now().minusDays(60);
        LocalDateTime customEnd = LocalDateTime.now().minusDays(30);
        
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(eq(projectId), eq(customStart), eq(customEnd)))
            .thenReturn(Collections.emptyList());
        when(qualityMetricsRepository.getQualityTrend(eq(projectId), eq(customStart), eq(customEnd)))
            .thenReturn(null);
        when(qualityMetricsRepository.getQualityTrendByDate(eq(projectId), eq(customStart), eq(customEnd)))
            .thenReturn(Collections.emptyList());
        when(qualityMetricsRepository.getQualityGateStats(eq(projectId), eq(customStart), eq(customEnd)))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/quality")
                .param("projectId", projectId)
                .param("startDate", customStart.toString())
                .param("endDate", customEnd.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.startDate").value(customStart.toString()))
                .andExpect(jsonPath("$.endDate").value(customEnd.toString()));

        verify(qualityMetricsRepository).findByProjectIdAndTimestampBetween(eq(projectId), eq(customStart), eq(customEnd));
    }

    @Test
    public void testGetTestCoverage_Success() throws Exception {
        // Given
        List<TestCoverage> coverageRecords = createMockTestCoverage();
        TestCoverageService.CoverageTrend trendData = createMockCoverageTrend();
        List<Object[]> coverageStatusStats = createMockCoverageStatusStats();

        when(testCoverageRepository.findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(coverageRecords);
        when(testCoverageService.getCoverageTrend(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(trendData);
        when(testCoverageRepository.getCoverageStatusStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(coverageStatusStats);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/coverage")
                .param("projectId", projectId)
                .param("timeRange", "30d")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.timeRange").value("30d"))
                .andExpect(jsonPath("$.coverageRecords").isArray())
                .andExpect(jsonPath("$.trendData").exists())
                .andExpect(jsonPath("$.overview").exists());

        verify(testCoverageRepository).findByProjectIdAndTimestampBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(testCoverageService).getCoverageTrend(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    public void testGetDashboard_Success() throws Exception {
        // Given
        com.gitlab.metrics.dto.DashboardResponse dashboardResponse = createMockDashboardResponse();
        when(dashboardService.getDashboardData(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class), eq("30d")))
            .thenReturn(dashboardResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/dashboard")
                .param("projectId", projectId)
                .param("timeRange", "30d")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.timeRange").value("30d"));

        verify(dashboardService).getDashboardData(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class), eq("30d"));
    }

    @Test
    public void testGetProjectOverview_Success() throws Exception {
        // Given
        CommitStatisticsService.ProjectTotalStats commitStats = createMockProjectTotalStats();
        List<QualityMetrics> latestQuality = createMockQualityMetrics();
        TestCoverage latestCoverage = createMockTestCoverage().get(0);

        when(commitStatisticsService.getProjectTotalStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(commitStats);
        when(qualityMetricsRepository.findLatestByProject(projectId))
            .thenReturn(latestQuality);
        when(testCoverageService.getLatestCoverage(projectId))
            .thenReturn(Optional.of(latestCoverage));

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/overview")
                .param("projectId", projectId)
                .param("timeRange", "30d")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.timeRange").value("30d"))
                .andExpect(jsonPath("$.commits").exists())
                .andExpect(jsonPath("$.latestQuality").exists())
                .andExpect(jsonPath("$.latestCoverage").exists());

        verify(commitStatisticsService).getProjectTotalStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(qualityMetricsRepository).findLatestByProject(projectId);
        verify(testCoverageService).getLatestCoverage(projectId);
    }

    @Test
    public void testCompareProjects_Success() throws Exception {
        // Given
        String projectIds = "project1,project2";
        CommitStatisticsService.ProjectTotalStats commitStats = createMockProjectTotalStats();
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        List<QualityMetrics> latestQuality = createMockQualityMetrics();
        TestCoverage latestCoverage = createMockTestCoverage().get(0);

        when(commitStatisticsService.getProjectTotalStats(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(commitStats);
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), anyString(), isNull()))
            .thenReturn(developerStats);
        when(qualityMetricsRepository.findLatestByProject(anyString()))
            .thenReturn(latestQuality);
        when(testCoverageService.getLatestCoverage(anyString()))
            .thenReturn(Optional.of(latestCoverage));

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/compare")
                .param("projectIds", projectIds)
                .param("timeRange", "30d")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timeRange").value("30d"))
                .andExpect(jsonPath("$.projects").isArray())
                .andExpect(jsonPath("$.projects").isNotEmpty());

        verify(commitStatisticsService, times(2)).getProjectTotalStats(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    public void testGetLeaderboard_Success() throws Exception {
        // Given
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = createMockDeveloperStats();
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
            .thenReturn(developerStats);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/leaderboard")
                .param("timeRange", "30d")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timeRange").value("30d"))
                .andExpect(jsonPath("$.commitLeaders").isArray())
                .andExpect(jsonPath("$.codeLeaders").isArray())
                .andExpect(jsonPath("$.fileLeaders").isArray());

        verify(commitStatisticsService).getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull());
    }

    @Test
    public void testGetRealtimeStats_Success() throws Exception {
        // Given
        List<CommitStatisticsService.ProjectCommitStats> todayCommits = createMockProjectStats();
        List<CommitStatisticsService.DeveloperCommitStats> todayDevelopers = createMockDeveloperStats();
        List<QualityMetrics> recentFailures = createMockQualityMetrics();
        List<TestCoverage> coverageFailures = createMockTestCoverage();

        when(commitStatisticsService.getProjectCommitStats(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(todayCommits);
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
            .thenReturn(todayDevelopers);
        when(qualityMetricsRepository.findFailedQualityGates(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(recentFailures);
        when(testCoverageRepository.findFailedCoverageRecords(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(coverageFailures);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/realtime")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.todayCommits").isNumber())
                .andExpect(jsonPath("$.activeProjects").isNumber())
                .andExpect(jsonPath("$.activeDevelopers").isNumber())
                .andExpect(jsonPath("$.qualityGateFailures").isNumber())
                .andExpect(jsonPath("$.coverageFailures").isNumber());

        verify(commitStatisticsService).getProjectCommitStats(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(commitStatisticsService).getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull());
    }

    @Test
    public void testGetCommitStats_ServiceException() throws Exception {
        // Given
        when(commitStatisticsService.getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/commits")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(commitStatisticsService).getDeveloperCommitStats(any(LocalDateTime.class), any(LocalDateTime.class), any(), any());
    }

    @Test
    public void testGetQualityMetrics_ServiceException() throws Exception {
        // Given
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(any(), any(), any()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/quality")
                .param("projectId", projectId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(qualityMetricsRepository).findByProjectIdAndTimestampBetween(any(), any(), any());
    }

    // Helper methods to create mock data

    private CommitStatisticsService.ProjectTotalStats createMockProjectTotalStats() {
        CommitStatisticsService.ProjectTotalStats stats = new CommitStatisticsService.ProjectTotalStats();
        stats.setTotalCommits(100);
        stats.setTotalLinesAdded(1000);
        stats.setTotalLinesDeleted(500);
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

    private List<CommitStatisticsService.ProjectCommitStats> createMockProjectStats() {
        CommitStatisticsService.ProjectCommitStats project1 = new CommitStatisticsService.ProjectCommitStats();
        project1.setProjectId("project1");
        project1.setCommitCount(50);
        project1.setLinesAdded(500);
        project1.setLinesDeleted(250);

        CommitStatisticsService.ProjectCommitStats project2 = new CommitStatisticsService.ProjectCommitStats();
        project2.setProjectId("project2");
        project2.setCommitCount(30);
        project2.setLinesAdded(300);
        project2.setLinesDeleted(150);

        return Arrays.asList(project1, project2);
    }

    private List<CommitStatisticsService.CommitTrendData> createMockTrendData() {
        CommitStatisticsService.CommitTrendData trend1 = new CommitStatisticsService.CommitTrendData();
        trend1.setDate(java.time.LocalDate.now().minusDays(2));
        trend1.setCommitCount(10);
        trend1.setLinesAdded(100);
        trend1.setLinesDeleted(50);

        CommitStatisticsService.CommitTrendData trend2 = new CommitStatisticsService.CommitTrendData();
        trend2.setDate(java.time.LocalDate.now().minusDays(1));
        trend2.setCommitCount(15);
        trend2.setLinesAdded(150);
        trend2.setLinesDeleted(75);

        return Arrays.asList(trend1, trend2);
    }

    private List<QualityMetrics> createMockQualityMetrics() {
        QualityMetrics metrics1 = new QualityMetrics();
        metrics1.setId(1L);
        metrics1.setProjectId(projectId);
        metrics1.setCommitSha("commit1");
        metrics1.setTimestamp(LocalDateTime.now().minusDays(1));
        metrics1.setBugs(2);
        metrics1.setVulnerabilities(1);
        metrics1.setCodeSmells(10);

        QualityMetrics metrics2 = new QualityMetrics();
        metrics2.setId(2L);
        metrics2.setProjectId(projectId);
        metrics2.setCommitSha("commit2");
        metrics2.setTimestamp(LocalDateTime.now().minusDays(2));
        metrics2.setBugs(1);
        metrics2.setVulnerabilities(0);
        metrics2.setCodeSmells(8);

        return Arrays.asList(metrics1, metrics2);
    }

    private List<Object[]> createMockQualityTrendByDate() {
        return Arrays.asList(
            new Object[]{java.time.LocalDate.now().minusDays(2), 5.0, 3.0, 80.0, 10.0},
            new Object[]{java.time.LocalDate.now().minusDays(1), 4.0, 2.0, 85.0, 8.0}
        );
    }

    private List<Object[]> createMockQualityGateStats() {
        return Arrays.asList(
            new Object[]{"PASSED", 8L},
            new Object[]{"FAILED", 2L}
        );
    }

    private List<TestCoverage> createMockTestCoverage() {
        TestCoverage coverage1 = new TestCoverage();
        coverage1.setId(1L);
        coverage1.setProjectId(projectId);
        coverage1.setCommitSha("commit1");
        coverage1.setTimestamp(LocalDateTime.now().minusDays(1));
        coverage1.setLineCoverage(85.0);
        coverage1.setBranchCoverage(80.0);
        coverage1.setFunctionCoverage(90.0);

        TestCoverage coverage2 = new TestCoverage();
        coverage2.setId(2L);
        coverage2.setProjectId(projectId);
        coverage2.setCommitSha("commit2");
        coverage2.setTimestamp(LocalDateTime.now().minusDays(2));
        coverage2.setLineCoverage(88.0);
        coverage2.setBranchCoverage(82.0);
        coverage2.setFunctionCoverage(92.0);

        return Arrays.asList(coverage1, coverage2);
    }

    private TestCoverageService.CoverageTrend createMockCoverageTrend() {
        TestCoverageService.CoverageTrend trend = new TestCoverageService.CoverageTrend();
        trend.setProjectId(projectId);
        trend.setStartDate(startDate);
        trend.setEndDate(endDate);
        trend.setAverageLineCoverage(86.5);
        trend.setAverageBranchCoverage(81.0);
        trend.setAverageFunctionCoverage(91.0);
        return trend;
    }

    private List<Object[]> createMockCoverageStatusStats() {
        return Arrays.asList(
            new Object[]{"PASSED", 7L},
            new Object[]{"FAILED", 3L}
        );
    }

    private com.gitlab.metrics.dto.DashboardResponse createMockDashboardResponse() {
        com.gitlab.metrics.dto.DashboardResponse response = new com.gitlab.metrics.dto.DashboardResponse(projectId, "30d");
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        return response;
    }
}