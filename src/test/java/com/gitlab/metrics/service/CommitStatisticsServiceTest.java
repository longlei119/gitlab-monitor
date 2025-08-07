package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.service.CommitStatisticsService.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 代码提交统计服务测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class CommitStatisticsServiceTest {
    
    @Mock
    private CommitRepository commitRepository;
    
    @InjectMocks
    private CommitStatisticsService commitStatisticsService;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Before
    public void setUp() {
        startDate = LocalDateTime.of(2023, 12, 1, 0, 0);
        endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
    }
    
    @Test
    public void testGetDeveloperCommitStats_WithoutProjectFilter() {
        // 准备测试数据
        Object[] row1 = {"dev1@example.com", "Developer One", 10L, 500L, 200L, 15L};
        Object[] row2 = {"dev2@example.com", "Developer Two", 8L, 300L, 150L, 12L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(commitRepository.getDeveloperCommitStats(startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<DeveloperCommitStats> result = commitStatisticsService.getDeveloperCommitStats(
            startDate, endDate, null, null);
        
        // 验证结果
        assertEquals(2, result.size());
        
        DeveloperCommitStats stats1 = result.get(0);
        assertEquals("dev1@example.com", stats1.getDeveloperId());
        assertEquals("Developer One", stats1.getDeveloperName());
        assertEquals(Integer.valueOf(10), stats1.getCommitCount());
        assertEquals(Integer.valueOf(500), stats1.getLinesAdded());
        assertEquals(Integer.valueOf(200), stats1.getLinesDeleted());
        assertEquals(Integer.valueOf(15), stats1.getFilesChanged());
        
        verify(commitRepository, times(1)).getDeveloperCommitStats(startDate, endDate);
        verify(commitRepository, never()).getDeveloperCommitStatsByProject(anyString(), any(), any());
    }
    
    @Test
    public void testGetDeveloperCommitStats_WithProjectFilter() {
        // 准备测试数据
        String projectId = "project123";
        Object[] row1 = {"dev1@example.com", "Developer One", 5L, 250L, 100L, 8L};
        List<Object[]> mockData = Collections.singletonList(row1);
        
        when(commitRepository.getDeveloperCommitStatsByProject(projectId, startDate, endDate))
            .thenReturn(mockData);
        
        // 执行测试
        List<DeveloperCommitStats> result = commitStatisticsService.getDeveloperCommitStats(
            startDate, endDate, projectId, null);
        
        // 验证结果
        assertEquals(1, result.size());
        
        DeveloperCommitStats stats = result.get(0);
        assertEquals("dev1@example.com", stats.getDeveloperId());
        assertEquals("Developer One", stats.getDeveloperName());
        assertEquals(Integer.valueOf(5), stats.getCommitCount());
        
        verify(commitRepository, times(1)).getDeveloperCommitStatsByProject(projectId, startDate, endDate);
        verify(commitRepository, never()).getDeveloperCommitStats(any(), any());
    }
    
    @Test
    public void testGetDeveloperCommitStats_WithDeveloperFilter() {
        // 准备测试数据
        String targetDeveloperId = "dev1@example.com";
        Object[] row1 = {"dev1@example.com", "Developer One", 10L, 500L, 200L, 15L};
        Object[] row2 = {"dev2@example.com", "Developer Two", 8L, 300L, 150L, 12L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(commitRepository.getDeveloperCommitStats(startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<DeveloperCommitStats> result = commitStatisticsService.getDeveloperCommitStats(
            startDate, endDate, null, targetDeveloperId);
        
        // 验证结果 - 应该只返回指定开发者的数据
        assertEquals(1, result.size());
        assertEquals(targetDeveloperId, result.get(0).getDeveloperId());
        
        verify(commitRepository, times(1)).getDeveloperCommitStats(startDate, endDate);
    }
    
    @Test
    public void testGetProjectCommitStats() {
        // 准备测试数据
        Object[] row1 = {"project1", 20L, 1000L, 400L, 30L};
        Object[] row2 = {"project2", 15L, 750L, 300L, 25L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(commitRepository.getProjectCommitStats(startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<ProjectCommitStats> result = commitStatisticsService.getProjectCommitStats(startDate, endDate);
        
        // 验证结果
        assertEquals(2, result.size());
        
        ProjectCommitStats stats1 = result.get(0);
        assertEquals("project1", stats1.getProjectId());
        assertEquals(Integer.valueOf(20), stats1.getCommitCount());
        assertEquals(Integer.valueOf(1000), stats1.getLinesAdded());
        assertEquals(Integer.valueOf(400), stats1.getLinesDeleted());
        assertEquals(Integer.valueOf(30), stats1.getFilesChanged());
        
        verify(commitRepository, times(1)).getProjectCommitStats(startDate, endDate);
    }
    
    @Test
    public void testGetCommitTrend_WithoutProjectFilter() {
        // 准备测试数据
        LocalDate date1 = LocalDate.of(2023, 12, 1);
        LocalDate date2 = LocalDate.of(2023, 12, 2);
        Object[] row1 = {date1, 5L, 250L, 100L};
        Object[] row2 = {date2, 3L, 150L, 50L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(commitRepository.getCommitTrendByDate(startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<CommitTrendData> result = commitStatisticsService.getCommitTrend(startDate, endDate, null);
        
        // 验证结果 - 应该包含填充的缺失日期
        assertTrue(result.size() >= 2); // 至少包含有数据的两天
        
        // 验证第一天的数据
        CommitTrendData firstDay = result.stream()
            .filter(data -> data.getDate().equals(date1))
            .findFirst()
            .orElse(null);
        assertNotNull(firstDay);
        assertEquals(Integer.valueOf(5), firstDay.getCommitCount());
        assertEquals(Integer.valueOf(250), firstDay.getLinesAdded());
        assertEquals(Integer.valueOf(100), firstDay.getLinesDeleted());
        
        verify(commitRepository, times(1)).getCommitTrendByDate(startDate, endDate);
        verify(commitRepository, never()).getCommitTrendByProjectAndDate(anyString(), any(), any());
    }
    
    @Test
    public void testGetCommitTrend_WithProjectFilter() {
        // 准备测试数据
        String projectId = "project123";
        LocalDate date1 = LocalDate.of(2023, 12, 1);
        Object[] row1 = {projectId, date1, 3L, 150L, 75L};
        List<Object[]> mockData = Collections.singletonList(row1);
        
        when(commitRepository.getCommitTrendByProjectAndDate(projectId, startDate, endDate))
            .thenReturn(mockData);
        
        // 执行测试
        List<CommitTrendData> result = commitStatisticsService.getCommitTrend(startDate, endDate, projectId);
        
        // 验证结果
        assertTrue(result.size() >= 1);
        
        CommitTrendData firstDay = result.stream()
            .filter(data -> data.getDate().equals(date1))
            .findFirst()
            .orElse(null);
        assertNotNull(firstDay);
        assertEquals(projectId, firstDay.getProjectId());
        assertEquals(Integer.valueOf(3), firstDay.getCommitCount());
        
        verify(commitRepository, times(1)).getCommitTrendByProjectAndDate(projectId, startDate, endDate);
        verify(commitRepository, never()).getCommitTrendByDate(any(), any());
    }
    
    @Test
    public void testGetBranchActivityStats() {
        // 准备测试数据
        String projectId = "project123";
        Object[] row1 = {"main", 15L, 750L, 300L};
        Object[] row2 = {"develop", 10L, 500L, 200L};
        Object[] row3 = {"feature/new-feature", 5L, 250L, 100L};
        List<Object[]> mockData = Arrays.asList(row1, row2, row3);
        
        when(commitRepository.getBranchActivityStats(projectId, startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<BranchActivityStats> result = commitStatisticsService.getBranchActivityStats(projectId, startDate, endDate);
        
        // 验证结果
        assertEquals(3, result.size());
        
        BranchActivityStats mainBranch = result.get(0);
        assertEquals("main", mainBranch.getBranchName());
        assertEquals(Integer.valueOf(15), mainBranch.getCommitCount());
        assertEquals(Integer.valueOf(750), mainBranch.getLinesAdded());
        assertEquals(Integer.valueOf(300), mainBranch.getLinesDeleted());
        
        verify(commitRepository, times(1)).getBranchActivityStats(projectId, startDate, endDate);
    }
    
    @Test
    public void testGetDeveloperActivityByHour() {
        // 准备测试数据 - 只有几个小时有提交
        String developerId = "dev1@example.com";
        Object[] row1 = {9, 5L}; // 9点有5次提交
        Object[] row2 = {14, 3L}; // 14点有3次提交
        Object[] row3 = {18, 2L}; // 18点有2次提交
        List<Object[]> mockData = Arrays.asList(row1, row2, row3);
        
        when(commitRepository.getDeveloperActivityByHour(developerId, startDate, endDate))
            .thenReturn(mockData);
        
        // 执行测试
        List<DeveloperActivityData> result = commitStatisticsService.getDeveloperActivityByHour(
            developerId, startDate, endDate);
        
        // 验证结果 - 应该返回24小时的数据
        assertEquals(24, result.size());
        
        // 验证有数据的小时
        DeveloperActivityData hour9 = result.get(9);
        assertEquals(Integer.valueOf(9), hour9.getHour());
        assertEquals(Integer.valueOf(5), hour9.getCommitCount());
        
        DeveloperActivityData hour14 = result.get(14);
        assertEquals(Integer.valueOf(14), hour14.getHour());
        assertEquals(Integer.valueOf(3), hour14.getCommitCount());
        
        // 验证没有数据的小时应该为0
        DeveloperActivityData hour0 = result.get(0);
        assertEquals(Integer.valueOf(0), hour0.getHour());
        assertEquals(Integer.valueOf(0), hour0.getCommitCount());
        
        verify(commitRepository, times(1)).getDeveloperActivityByHour(developerId, startDate, endDate);
    }
    
    @Test
    public void testGetLargeCommits() {
        // 准备测试数据
        Integer threshold = 100;
        Commit commit1 = new Commit();
        commit1.setCommitSha("abc123");
        commit1.setLinesAdded(150);
        commit1.setLinesDeleted(50);
        
        Commit commit2 = new Commit();
        commit2.setCommitSha("def456");
        commit2.setLinesAdded(80);
        commit2.setLinesDeleted(30);
        
        List<Commit> mockCommits = Arrays.asList(commit1, commit2);
        
        when(commitRepository.findLargeCommits(threshold, startDate, endDate)).thenReturn(mockCommits);
        
        // 执行测试
        List<Commit> result = commitStatisticsService.getLargeCommits(threshold, startDate, endDate);
        
        // 验证结果
        assertEquals(2, result.size());
        assertEquals("abc123", result.get(0).getCommitSha());
        assertEquals("def456", result.get(1).getCommitSha());
        
        verify(commitRepository, times(1)).findLargeCommits(threshold, startDate, endDate);
    }
    
    @Test
    public void testGetDeveloperAverageCommitSize() {
        // 准备测试数据
        Object[] row1 = {"dev1@example.com", "Developer One", 50.5, 3.2};
        Object[] row2 = {"dev2@example.com", "Developer Two", 75.8, 4.1};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        when(commitRepository.getDeveloperAverageCommitSize(startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        List<DeveloperAverageCommitSize> result = commitStatisticsService.getDeveloperAverageCommitSize(
            startDate, endDate);
        
        // 验证结果
        assertEquals(2, result.size());
        
        DeveloperAverageCommitSize stats1 = result.get(0);
        assertEquals("dev1@example.com", stats1.getDeveloperId());
        assertEquals("Developer One", stats1.getDeveloperName());
        assertEquals(Integer.valueOf(50), stats1.getAverageChangedLines()); // 转换为整数
        assertEquals(Integer.valueOf(3), stats1.getAverageChangedFiles());
        
        verify(commitRepository, times(1)).getDeveloperAverageCommitSize(startDate, endDate);
    }
    
    @Test
    public void testGetProjectTotalStats() {
        // 准备测试数据
        String projectId = "project123";
        Object[] mockData = {1000L, 400L, 50L, 25L}; // linesAdded, linesDeleted, filesChanged, commits
        
        when(commitRepository.getProjectTotalStats(projectId, startDate, endDate)).thenReturn(mockData);
        
        // 执行测试
        ProjectTotalStats result = commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
        
        // 验证结果
        assertEquals(Integer.valueOf(1000), result.getTotalLinesAdded());
        assertEquals(Integer.valueOf(400), result.getTotalLinesDeleted());
        assertEquals(Integer.valueOf(50), result.getTotalFilesChanged());
        assertEquals(Integer.valueOf(25), result.getTotalCommits());
        
        verify(commitRepository, times(1)).getProjectTotalStats(projectId, startDate, endDate);
    }
    
    @Test
    public void testGetDeveloperCommitStats_EmptyResult() {
        // 准备空的测试数据
        when(commitRepository.getDeveloperCommitStats(startDate, endDate)).thenReturn(Collections.emptyList());
        
        // 执行测试
        List<DeveloperCommitStats> result = commitStatisticsService.getDeveloperCommitStats(
            startDate, endDate, null, null);
        
        // 验证结果
        assertEquals(0, result.size());
        
        verify(commitRepository, times(1)).getDeveloperCommitStats(startDate, endDate);
    }
    
    @Test
    public void testGetCommitTrend_FillMissingDates() {
        // 准备测试数据 - 只有部分日期有数据
        LocalDate date1 = LocalDate.of(2023, 12, 1);
        LocalDate date3 = LocalDate.of(2023, 12, 3); // 跳过12月2日
        Object[] row1 = {date1, 5L, 250L, 100L};
        Object[] row2 = {date3, 3L, 150L, 50L};
        List<Object[]> mockData = Arrays.asList(row1, row2);
        
        // 设置较短的时间范围以便测试
        LocalDateTime shortStartDate = LocalDateTime.of(2023, 12, 1, 0, 0);
        LocalDateTime shortEndDate = LocalDateTime.of(2023, 12, 3, 23, 59);
        
        when(commitRepository.getCommitTrendByDate(shortStartDate, shortEndDate)).thenReturn(mockData);
        
        // 执行测试
        List<CommitTrendData> result = commitStatisticsService.getCommitTrend(shortStartDate, shortEndDate, null);
        
        // 验证结果 - 应该包含3天的数据（包括缺失的12月2日）
        assertEquals(3, result.size());
        
        // 验证12月2日的数据被填充为0
        CommitTrendData missingDay = result.stream()
            .filter(data -> data.getDate().equals(LocalDate.of(2023, 12, 2)))
            .findFirst()
            .orElse(null);
        assertNotNull(missingDay);
        assertEquals(Integer.valueOf(0), missingDay.getCommitCount());
        assertEquals(Integer.valueOf(0), missingDay.getLinesAdded());
        assertEquals(Integer.valueOf(0), missingDay.getLinesDeleted());
        
        verify(commitRepository, times(1)).getCommitTrendByDate(shortStartDate, shortEndDate);
    }
}