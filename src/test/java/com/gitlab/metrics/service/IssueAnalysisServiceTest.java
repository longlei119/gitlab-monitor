package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.repository.IssueRepository;
import com.gitlab.metrics.service.IssueAnalysisService.IssueProcessResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IssueAnalysisService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class IssueAnalysisServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private BugFixEfficiencyService bugFixEfficiencyService;

    @InjectMocks
    private IssueAnalysisService issueAnalysisService;

    private IssueEventRequest issueEvent;
    private Issue existingIssue;

    @Before
    public void setUp() {
        // 准备测试数据
        issueEvent = createMockIssueEvent();
        existingIssue = createMockIssue();
    }

    @Test
    public void testProcessIssueEvent_OpenAction() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("123", result.getIssueId());
        assertEquals("open", result.getAction());
        assertEquals("Issue创建成功", result.getMessage());
        assertNotNull(result.getIssue());
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    public void testProcessIssueEvent_OpenAction_IssueExists() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(existingIssue));

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Issue已存在", result.getMessage());
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    public void testProcessIssueEvent_UpdateAction() {
        // Given
        issueEvent.getObjectAttributes().setAction("update");
        issueEvent.getObjectAttributes().setState("in_progress");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(existingIssue));
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Issue更新成功", result.getMessage());
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    public void testProcessIssueEvent_UpdateAction_IssueNotExists() {
        // Given
        issueEvent.getObjectAttributes().setAction("update");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Issue创建成功", result.getMessage());
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    public void testProcessIssueEvent_CloseAction() {
        // Given
        issueEvent.getObjectAttributes().setAction("close");
        issueEvent.getObjectAttributes().setClosedAt("2023-12-01T15:30:00");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(existingIssue));
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Issue关闭成功", result.getMessage());
        
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertEquals("closed", savedIssue.getStatus());
        assertNotNull(savedIssue.getClosedAt());
        assertNotNull(savedIssue.getResolutionAt());
    }

    @Test
    public void testProcessIssueEvent_CloseAction_IssueNotExists() {
        // Given
        issueEvent.getObjectAttributes().setAction("close");
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Issue不存在", result.getMessage());
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    public void testProcessIssueEvent_ReopenAction() {
        // Given
        issueEvent.getObjectAttributes().setAction("reopen");
        existingIssue.setStatus("closed");
        existingIssue.setClosedAt(LocalDateTime.now());
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(existingIssue));
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Issue重新打开成功", result.getMessage());
        
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertEquals("opened", savedIssue.getStatus());
        assertNull(savedIssue.getClosedAt());
        assertNull(savedIssue.getResolutionAt());
        assertNull(savedIssue.getResolutionTimeMinutes());
    }

    @Test
    public void testProcessIssueEvent_UnsupportedAction() {
        // Given
        issueEvent.getObjectAttributes().setAction("unknown");

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("未支持的事件类型"));
    }

    @Test
    public void testProcessIssueEvent_Exception() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        when(issueRepository.findByIssueId(anyString())).thenThrow(new RuntimeException("Database error"));

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("处理失败"));
    }

    @Test
    public void testAnalyzeIssueTypeAndSeverity_Bug() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        issueEvent.getObjectAttributes().setTitle("Fix critical bug in payment system");
        issueEvent.getObjectAttributes().setDescription("This is a critical bug that causes data loss");
        issueEvent.getObjectAttributes().setLabels(new String[]{"bug", "critical"});
        
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertEquals("bug", savedIssue.getIssueType());
        assertEquals("critical", savedIssue.getPriority());
        assertEquals("blocker", savedIssue.getSeverity());
    }

    @Test
    public void testAnalyzeIssueTypeAndSeverity_Feature() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        issueEvent.getObjectAttributes().setTitle("Add new feature for user management");
        issueEvent.getObjectAttributes().setDescription("Enhancement to improve user experience");
        issueEvent.getObjectAttributes().setLabels(new String[]{"feature", "enhancement"});
        
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertEquals("feature", savedIssue.getIssueType());
        assertEquals("medium", savedIssue.getPriority());
        assertEquals("minor", savedIssue.getSeverity());
    }

    @Test
    public void testAnalyzeIssueTypeAndSeverity_Task() {
        // Given
        issueEvent.getObjectAttributes().setAction("open");
        issueEvent.getObjectAttributes().setTitle("Update documentation");
        issueEvent.getObjectAttributes().setDescription("Regular maintenance task");
        issueEvent.getObjectAttributes().setLabels(new String[]{"task", "low"});
        
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenReturn(existingIssue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertEquals("task", savedIssue.getIssueType());
        assertEquals("low", savedIssue.getPriority());
        assertEquals("minor", savedIssue.getSeverity());
    }

    @Test
    public void testGetBugFixEfficiencyStats() {
        // Given
        String projectId = "test-project";
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        BugFixEfficiencyStats mockStats = new BugFixEfficiencyStats();
        mockStats.setProjectId(projectId);
        mockStats.setTotalBugs(10);
        mockStats.setFixedBugs(8);
        
        when(bugFixEfficiencyService.calculateBugFixEfficiency(projectId, null, start, end))
            .thenReturn(mockStats);

        // When
        BugFixEfficiencyStats result = issueAnalysisService.getBugFixEfficiencyStats(projectId, start, end);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(10, result.getTotalBugs());
        assertEquals(8, result.getFixedBugs());
        verify(bugFixEfficiencyService).calculateBugFixEfficiency(projectId, null, start, end);
    }

    @Test
    public void testGetBugFixEfficiencyStats_Exception() {
        // Given
        String projectId = "test-project";
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        when(bugFixEfficiencyService.calculateBugFixEfficiency(anyString(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            issueAnalysisService.getBugFixEfficiencyStats(projectId, start, end);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("获取Bug修复效率统计失败", e.getMessage());
        }
    }

    @Test
    public void testGetLongPendingIssues_WithProjectId() {
        // Given
        String projectId = "test-project";
        Integer hoursThreshold = 48;
        List<Issue> mockIssues = Arrays.asList(existingIssue, createMockIssue());
        
        when(issueRepository.findLongPendingIssuesByProject(projectId, hoursThreshold))
            .thenReturn(mockIssues);

        // When
        List<Issue> result = issueAnalysisService.getLongPendingIssues(projectId, hoursThreshold);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(issueRepository).findLongPendingIssuesByProject(projectId, hoursThreshold);
        verify(issueRepository, never()).findLongPendingIssues(anyInt());
    }

    @Test
    public void testGetLongPendingIssues_WithoutProjectId() {
        // Given
        String projectId = null;
        Integer hoursThreshold = 48;
        List<Issue> mockIssues = Arrays.asList(existingIssue);
        
        when(issueRepository.findLongPendingIssues(hoursThreshold))
            .thenReturn(mockIssues);

        // When
        List<Issue> result = issueAnalysisService.getLongPendingIssues(projectId, hoursThreshold);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(issueRepository).findLongPendingIssues(hoursThreshold);
        verify(issueRepository, never()).findLongPendingIssuesByProject(anyString(), anyInt());
    }

    @Test
    public void testGetLongPendingIssues_Exception() {
        // Given
        String projectId = "test-project";
        Integer hoursThreshold = 48;
        
        when(issueRepository.findLongPendingIssuesByProject(anyString(), anyInt()))
            .thenThrow(new RuntimeException("Database error"));

        // When
        List<Issue> result = issueAnalysisService.getLongPendingIssues(projectId, hoursThreshold);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCalculateResponseTime() {
        // Given
        issueEvent.getObjectAttributes().setAction("update");
        issueEvent.getObjectAttributes().setAssigneeId(456L);
        
        Issue issue = createMockIssue();
        issue.setCreatedAt(LocalDateTime.now().minusHours(2));
        issue.setFirstResponseAt(null);
        
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertNotNull(savedIssue.getFirstResponseAt());
        assertNotNull(savedIssue.getResponseTimeMinutes());
        assertTrue(savedIssue.getResponseTimeMinutes() > 0);
    }

    @Test
    public void testCalculateResolutionTime() {
        // Given
        issueEvent.getObjectAttributes().setAction("close");
        issueEvent.getObjectAttributes().setClosedAt("2023-12-01T15:30:00");
        
        Issue issue = createMockIssue();
        issue.setCreatedAt(LocalDateTime.now().minusHours(5));
        
        when(issueRepository.findByIssueId("123")).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        // When
        IssueProcessResult result = issueAnalysisService.processIssueEvent(issueEvent);

        // Then
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        
        Issue savedIssue = issueCaptor.getValue();
        assertNotNull(savedIssue.getResolutionAt());
        assertNotNull(savedIssue.getResolutionTimeMinutes());
        assertTrue(savedIssue.getResolutionTimeMinutes() > 0);
    }

    @Test
    public void testIssueProcessResult() {
        // Test IssueProcessResult class
        IssueProcessResult result = new IssueProcessResult();
        result.setProjectId("test-project");
        result.setIssueId("123");
        result.setAction("open");
        result.setProcessTime(LocalDateTime.now());
        result.setSuccess(true);
        result.setMessage("Success");
        result.setIssue(existingIssue);

        assertEquals("test-project", result.getProjectId());
        assertEquals("123", result.getIssueId());
        assertEquals("open", result.getAction());
        assertNotNull(result.getProcessTime());
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertEquals(existingIssue, result.getIssue());
    }

    // Helper methods

    private IssueEventRequest createMockIssueEvent() {
        IssueEventRequest event = new IssueEventRequest();
        
        IssueEventRequest.Project project = new IssueEventRequest.Project();
        project.setId(1L);
        event.setProject(project);
        
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
        attrs.setLabels(new String[]{"bug"});
        event.setObjectAttributes(attrs);
        
        IssueEventRequest.User user = new IssueEventRequest.User();
        user.setId(100L);
        user.setName("Test User");
        event.setUser(user);
        
        IssueEventRequest.User assignee = new IssueEventRequest.User();
        assignee.setId(200L);
        assignee.setName("Test Assignee");
        event.setAssignee(assignee);
        
        return event;
    }

    private Issue createMockIssue() {
        Issue issue = new Issue();
        issue.setId(1L);
        issue.setIssueId("123");
        issue.setProjectId("1");
        issue.setTitle("Test Issue");
        issue.setDescription("Test Description");
        issue.setStatus("opened");
        issue.setCreatedAt(LocalDateTime.now().minusHours(1));
        issue.setUpdatedAt(LocalDateTime.now());
        issue.setAuthorId("100");
        issue.setAuthorName("Test User");
        issue.setIssueType("bug");
        issue.setPriority("medium");
        issue.setSeverity("minor");
        return issue;
    }
}