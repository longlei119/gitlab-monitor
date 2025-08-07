package com.gitlab.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.service.BugFixEfficiencyService;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bug修复效率控制器测试类
 */
@RunWith(SpringRunner.class)
@WebMvcTest(BugFixEfficiencyController.class)
public class BugFixEfficiencyControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BugFixEfficiencyService bugFixEfficiencyService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private BugFixEfficiencyStats testStats;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @Before
    public void setUp() {
        startTime = LocalDateTime.now().minusDays(30);
        endTime = LocalDateTime.now();
        
        // 创建测试统计数据
        testStats = createTestStats();
    }
    
    @Test
    public void testGetBugFixEfficiencyStats_Success() throws Exception {
        // Given
        String projectId = "project-1";
        when(bugFixEfficiencyService.calculateBugFixEfficiency(eq(projectId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testStats);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/stats")
                .param("projectId", projectId)
                .param("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.totalBugs").value(10))
                .andExpect(jsonPath("$.closedBugs").value(8))
                .andExpect(jsonPath("$.openBugs").value(2))
                .andExpect(jsonPath("$.resolutionRate").value(80.0))
                .andExpect(jsonPath("$.averageResolutionTimeHours").value(24.5))
                .andExpect(jsonPath("$.severityStats").isArray())
                .andExpect(jsonPath("$.priorityStats").isArray());
        
        verify(bugFixEfficiencyService).calculateBugFixEfficiency(eq(projectId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testGetBugFixEfficiencyStats_WithAssignee() throws Exception {
        // Given
        String projectId = "project-1";
        String assigneeId = "user-1";
        when(bugFixEfficiencyService.calculateBugFixEfficiency(eq(projectId), eq(assigneeId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testStats);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/stats")
                .param("projectId", projectId)
                .param("assigneeId", assigneeId)
                .param("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId));
        
        verify(bugFixEfficiencyService).calculateBugFixEfficiency(eq(projectId), eq(assigneeId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testGetBugFixEfficiencyStats_MissingRequiredParams() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/stats")
                .param("projectId", "project-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(bugFixEfficiencyService);
    }
    
    @Test
    public void testCompareBugFixEfficiency_Success() throws Exception {
        // Given
        String projectId = "project-1";
        LocalDateTime period1Start = LocalDateTime.now().minusDays(60);
        LocalDateTime period1End = LocalDateTime.now().minusDays(30);
        LocalDateTime period2Start = LocalDateTime.now().minusDays(30);
        LocalDateTime period2End = LocalDateTime.now();
        
        BugFixEfficiencyService.BugFixEfficiencyComparison comparison = createTestComparison();
        when(bugFixEfficiencyService.compareBugFixEfficiency(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(comparison);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/compare")
                .param("projectId", projectId)
                .param("period1Start", period1Start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("period1End", period1End.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("period2Start", period2Start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("period2End", period2End.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.overallAssessment").value("显著改善"))
                .andExpect(jsonPath("$.resolutionRateImprovement").value(10.0));
        
        verify(bugFixEfficiencyService).compareBugFixEfficiency(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testGetLongPendingBugs_Success() throws Exception {
        // Given
        String projectId = "project-1";
        Integer hoursThreshold = 72;
        List<Issue> longPendingBugs = createTestLongPendingBugs();
        
        when(bugFixEfficiencyService.getLongPendingIssues(eq(projectId), eq(hoursThreshold)))
            .thenReturn(longPendingBugs);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/long-pending")
                .param("projectId", projectId)
                .param("hoursThreshold", hoursThreshold.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].issueId").value("1"))
                .andExpect(jsonPath("$[0].status").value("opened"))
                .andExpect(jsonPath("$[1].issueId").value("2"));
        
        verify(bugFixEfficiencyService).getLongPendingIssues(eq(projectId), eq(hoursThreshold));
    }
    
    @Test
    public void testGetLongPendingBugs_WithDefaultThreshold() throws Exception {
        // Given
        List<Issue> longPendingBugs = createTestLongPendingBugs();
        when(bugFixEfficiencyService.getLongPendingIssues(isNull(), eq(72)))
            .thenReturn(longPendingBugs);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/long-pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        
        verify(bugFixEfficiencyService).getLongPendingIssues(isNull(), eq(72));
    }
    
    @Test
    public void testGetBugFixEfficiencyOverview_Success() throws Exception {
        // Given
        String projectId = "project-1";
        Integer days = 30;
        when(bugFixEfficiencyService.calculateBugFixEfficiency(eq(projectId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testStats);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/overview")
                .param("projectId", projectId)
                .param("days", days.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.days").value(days))
                .andExpect(jsonPath("$.totalBugs").value(10))
                .andExpect(jsonPath("$.closedBugs").value(8))
                .andExpect(jsonPath("$.openBugs").value(2))
                .andExpect(jsonPath("$.resolutionRate").value(80.0))
                .andExpect(jsonPath("$.timeoutBugs").value(1))
                .andExpect(jsonPath("$.efficiencyIssuesCount").value(2));
        
        verify(bugFixEfficiencyService).calculateBugFixEfficiency(eq(projectId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testGetBugFixEfficiencyOverview_WithDefaultDays() throws Exception {
        // Given
        when(bugFixEfficiencyService.calculateBugFixEfficiency(isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testStats);
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/overview")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30));
        
        verify(bugFixEfficiencyService).calculateBugFixEfficiency(isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testGetBugFixEfficiencyStats_ServiceException() throws Exception {
        // Given
        when(bugFixEfficiencyService.calculateBugFixEfficiency(any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        mockMvc.perform(get("/api/bug-fix-efficiency/stats")
                .param("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .param("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
    
    /**
     * 创建测试统计数据
     */
    private BugFixEfficiencyStats createTestStats() {
        BugFixEfficiencyStats stats = new BugFixEfficiencyStats();
        stats.setProjectId("project-1");
        stats.setStartTime(startTime);
        stats.setEndTime(endTime);
        stats.setCalculationTime(LocalDateTime.now());
        stats.setTotalBugs(10);
        stats.setClosedBugs(8);
        stats.setOpenBugs(2);
        stats.setResolutionRate(80.0);
        stats.setAverageResolutionTimeHours(24.5);
        stats.setMinResolutionTimeHours(2.0);
        stats.setMaxResolutionTimeHours(72.0);
        stats.setAverageResponseTimeHours(4.2);
        
        // 添加严重程度统计
        BugFixEfficiencyStats.SeverityStats criticalStats = new BugFixEfficiencyStats.SeverityStats();
        criticalStats.setSeverity("critical");
        criticalStats.setCount(3);
        criticalStats.setClosedCount(2);
        criticalStats.setResolutionRate(66.7);
        criticalStats.setAverageResolutionTimeHours(12.0);
        criticalStats.setAverageResponseTimeHours(2.0);
        criticalStats.setTimeoutCount(1);
        stats.getSeverityStats().add(criticalStats);
        
        // 添加优先级统计
        BugFixEfficiencyStats.PriorityStats highPriorityStats = new BugFixEfficiencyStats.PriorityStats();
        highPriorityStats.setPriority("high");
        highPriorityStats.setCount(4);
        highPriorityStats.setClosedCount(3);
        highPriorityStats.setResolutionRate(75.0);
        highPriorityStats.setAverageResolutionTimeHours(18.0);
        highPriorityStats.setAverageResponseTimeHours(3.0);
        stats.getPriorityStats().add(highPriorityStats);
        
        // 添加效率问题
        stats.getEfficiencyIssues().add("Average Bug response time is too long: 4.2 hours");
        stats.getEfficiencyIssues().add("critical level Bug has 1 timeout unresolved");
        
        return stats;
    }
    
    /**
     * 创建测试比较结果
     */
    private BugFixEfficiencyService.BugFixEfficiencyComparison createTestComparison() {
        BugFixEfficiencyService.BugFixEfficiencyComparison comparison = 
            new BugFixEfficiencyService.BugFixEfficiencyComparison();
        comparison.setProjectId("project-1");
        comparison.setPeriod1Stats(testStats);
        comparison.setPeriod2Stats(testStats);
        comparison.setComparisonTime(LocalDateTime.now());
        comparison.setResolutionRateImprovement(10.0);
        comparison.setFixTimeImprovement(5.0);
        comparison.setResponseTimeImprovement(2.0);
        comparison.setBugCountChange(-2);
        comparison.setOverallAssessment("显著改善");
        
        return comparison;
    }
    
    /**
     * 创建测试长时间未解决Bug列表
     */
    private List<Issue> createTestLongPendingBugs() {
        Issue bug1 = new Issue();
        bug1.setId(1L);
        bug1.setIssueId("1");
        bug1.setProjectId("project-1");
        bug1.setTitle("Long Pending Bug 1");
        bug1.setIssueType("bug");
        bug1.setStatus("opened");
        bug1.setSeverity("high");
        bug1.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        Issue bug2 = new Issue();
        bug2.setId(2L);
        bug2.setIssueId("2");
        bug2.setProjectId("project-1");
        bug2.setTitle("Long Pending Bug 2");
        bug2.setIssueType("bug");
        bug2.setStatus("opened");
        bug2.setSeverity("medium");
        bug2.setCreatedAt(LocalDateTime.now().minusDays(10));
        
        return Arrays.asList(bug1, bug2);
    }
}