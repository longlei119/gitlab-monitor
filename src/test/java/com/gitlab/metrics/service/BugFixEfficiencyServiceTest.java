package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.repository.IssueRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Bug修复效率服务测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class BugFixEfficiencyServiceTest {
    
    @Mock
    private IssueRepository issueRepository;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private BugFixEfficiencyService bugFixEfficiencyService;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Issue> testBugs;
    
    @Before
    public void setUp() {
        startTime = LocalDateTime.now().minusDays(30);
        endTime = LocalDateTime.now();
        
        // 设置配置参数
        ReflectionTestUtils.setField(bugFixEfficiencyService, "criticalBugTimeoutHours", 4);
        ReflectionTestUtils.setField(bugFixEfficiencyService, "highBugTimeoutHours", 24);
        ReflectionTestUtils.setField(bugFixEfficiencyService, "mediumBugTimeoutHours", 72);
        ReflectionTestUtils.setField(bugFixEfficiencyService, "lowBugTimeoutHours", 168);
        
        // 创建测试数据
        testBugs = createTestBugs();
    }
    
    @Test
    public void testCalculateBugFixEfficiency_WithValidData() {
        // Given
        String projectId = "project-1";
        when(issueRepository.findByProjectIdAndCreatedAtBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testBugs);
        
        // When
        BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
            projectId, null, startTime, endTime);
        
        // Then
        assertNotNull(stats);
        assertEquals(projectId, stats.getProjectId());
        assertEquals(3, stats.getTotalBugs());
        assertEquals(2, stats.getClosedBugs());
        assertEquals(1, stats.getOpenBugs());
        assertEquals(66.67, stats.getResolutionRate(), 0.1);
        assertTrue(stats.getAverageResolutionTimeHours() > 0);
        
        // 验证严重程度统计
        assertFalse(stats.getSeverityStats().isEmpty());
        
        // 验证优先级统计
        assertFalse(stats.getPriorityStats().isEmpty());
        
        verify(issueRepository).findByProjectIdAndCreatedAtBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testCalculateBugFixEfficiency_WithAssigneeFilter() {
        // Given
        String projectId = "project-1";
        String assigneeId = "user-1";
        List<Issue> assigneeBugs = testBugs.stream()
            .filter(bug -> assigneeId.equals(bug.getAssigneeId()))
            .collect(java.util.stream.Collectors.toList());
        
        when(issueRepository.findByAssigneeIdAndCreatedAtBetween(eq(assigneeId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(assigneeBugs);
        
        // When
        BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
            projectId, assigneeId, startTime, endTime);
        
        // Then
        assertNotNull(stats);
        assertEquals(projectId, stats.getProjectId());
        assertEquals(assigneeId, stats.getAssigneeId());
        assertTrue(stats.getTotalBugs() <= testBugs.size());
        
        verify(issueRepository).findByAssigneeIdAndCreatedAtBetween(eq(assigneeId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    public void testCalculateBugFixEfficiency_WithNoBugs() {
        // Given
        String projectId = "empty-project";
        when(issueRepository.findByProjectIdAndCreatedAtBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        
        // When
        BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
            projectId, null, startTime, endTime);
        
        // Then
        assertNotNull(stats);
        assertEquals(0, stats.getTotalBugs());
        assertEquals(0, stats.getClosedBugs());
        assertEquals(0, stats.getOpenBugs());
        assertEquals(0.0, stats.getResolutionRate(), 0.1);
    }
    
    @Test
    public void testCalculateBugFixEfficiency_WithSeverityStats() {
        // Given
        String projectId = "project-1";
        when(issueRepository.findByProjectIdAndCreatedAtBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testBugs);
        
        // When
        BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
            projectId, null, startTime, endTime);
        
        // Then
        assertNotNull(stats.getSeverityStats());
        assertFalse(stats.getSeverityStats().isEmpty());
        
        // 验证严重程度统计包含预期的数据
        boolean hasCritical = stats.getSeverityStats().stream()
            .anyMatch(s -> "critical".equals(s.getSeverity()));
        boolean hasMedium = stats.getSeverityStats().stream()
            .anyMatch(s -> "medium".equals(s.getSeverity()));
        boolean hasLow = stats.getSeverityStats().stream()
            .anyMatch(s -> "low".equals(s.getSeverity()));
        
        assertTrue("Should have critical severity stats", hasCritical);
        assertTrue("Should have medium severity stats", hasMedium);
        assertTrue("Should have low severity stats", hasLow);
    }
    
    @Test
    public void testCalculateBugFixEfficiency_WithDeveloperStats() {
        // Given
        String projectId = "project-1";
        when(issueRepository.findByProjectIdAndCreatedAtBetween(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testBugs);
        
        // When - 不指定assigneeId，这样会计算开发者统计
        BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
            projectId, null, startTime, endTime);
        
        // Then
        assertNotNull(stats.getDeveloperStats());
        assertFalse(stats.getDeveloperStats().isEmpty());
        
        // 验证开发者统计包含效率分数
        for (BugFixEfficiencyStats.DeveloperStats devStats : stats.getDeveloperStats()) {
            assertNotNull(devStats.getDeveloperId());
            assertTrue(devStats.getEfficiencyScore() >= 0);
        }
    }
    
    @Test
    public void testCheckTimeoutBugsAndSendAlerts() {
        // Given
        List<Issue> openBugs = Arrays.asList(createTimeoutBug());
        when(issueRepository.findByStatusOrderByCreatedAtDesc("opened"))
            .thenReturn(openBugs);
        
        // When
        bugFixEfficiencyService.checkTimeoutBugsAndSendAlerts();
        
        // Then
        verify(issueRepository).findByStatusOrderByCreatedAtDesc("opened");
        // 验证是否发送了告警（由于方法是私有的，这里主要验证没有异常）
    }
    
    /**
     * 创建测试Bug数据
     */
    private List<Issue> createTestBugs() {
        Issue bug1 = new Issue();
        bug1.setId(1L);
        bug1.setIssueId("1");
        bug1.setProjectId("project-1");
        bug1.setTitle("Critical Bug 1");
        bug1.setIssueType("bug");
        bug1.setStatus("closed");
        bug1.setSeverity("critical");
        bug1.setPriority("high");
        bug1.setAssigneeId("user-1");
        bug1.setAssigneeName("Developer 1");
        bug1.setCreatedAt(LocalDateTime.now().minusDays(5));
        bug1.setClosedAt(LocalDateTime.now().minusDays(4));
        bug1.setResolutionAt(LocalDateTime.now().minusDays(4));
        bug1.setResolutionTimeMinutes(1440L); // 24小时
        bug1.setFirstResponseAt(LocalDateTime.now().minusDays(5).plusHours(2));
        bug1.setResponseTimeMinutes(120L); // 2小时
        
        Issue bug2 = new Issue();
        bug2.setId(2L);
        bug2.setIssueId("2");
        bug2.setProjectId("project-1");
        bug2.setTitle("Medium Bug 2");
        bug2.setIssueType("bug");
        bug2.setStatus("closed");
        bug2.setSeverity("medium");
        bug2.setPriority("medium");
        bug2.setAssigneeId("user-2");
        bug2.setAssigneeName("Developer 2");
        bug2.setCreatedAt(LocalDateTime.now().minusDays(10));
        bug2.setClosedAt(LocalDateTime.now().minusDays(8));
        bug2.setResolutionAt(LocalDateTime.now().minusDays(8));
        bug2.setResolutionTimeMinutes(2880L); // 48小时
        bug2.setFirstResponseAt(LocalDateTime.now().minusDays(10).plusHours(4));
        bug2.setResponseTimeMinutes(240L); // 4小时
        
        Issue bug3 = new Issue();
        bug3.setId(3L);
        bug3.setIssueId("3");
        bug3.setProjectId("project-1");
        bug3.setTitle("Low Bug 3");
        bug3.setIssueType("bug");
        bug3.setStatus("opened");
        bug3.setSeverity("low");
        bug3.setPriority("low");
        bug3.setAssigneeId("user-1");
        bug3.setAssigneeName("Developer 1");
        bug3.setCreatedAt(LocalDateTime.now().minusDays(15));
        
        return Arrays.asList(bug1, bug2, bug3);
    }
    
    /**
     * 创建超时Bug数据
     */
    private Issue createTimeoutBug() {
        Issue timeoutBug = new Issue();
        timeoutBug.setId(4L);
        timeoutBug.setIssueId("4");
        timeoutBug.setProjectId("project-1");
        timeoutBug.setTitle("Timeout Critical Bug");
        timeoutBug.setIssueType("bug");
        timeoutBug.setStatus("opened");
        timeoutBug.setSeverity("critical");
        timeoutBug.setPriority("critical");
        timeoutBug.setAssigneeId("user-1");
        timeoutBug.setAssigneeName("Developer 1");
        timeoutBug.setCreatedAt(LocalDateTime.now().minusHours(6)); // 6小时前创建，超过critical的4小时阈值
        timeoutBug.setWebUrl("https://gitlab.example.com/issues/4");
        
        return timeoutBug;
    }
}