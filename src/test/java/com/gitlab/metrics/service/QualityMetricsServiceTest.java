package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import com.gitlab.metrics.service.QualityMetricsService.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QualityMetricsService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class QualityMetricsServiceTest {

    @Mock
    private QualityMetricsRepository qualityMetricsRepository;

    @InjectMocks
    private QualityMetricsService qualityMetricsService;

    private String projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private QualityMetrics qualityMetrics1;
    private QualityMetrics qualityMetrics2;

    @Before
    public void setUp() {
        projectId = "test-project";
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();

        qualityMetrics1 = createMockQualityMetrics("1", 5.0, 3.0, 80.0, 10.0);
        qualityMetrics2 = createMockQualityMetrics("2", 4.0, 2.5, 85.0, 8.0);
    }

    @Test
    public void testGetQualityHistory() {
        // Given
        List<QualityMetrics> mockMetrics = Arrays.asList(qualityMetrics1, qualityMetrics2);
        when(qualityMetricsRepository.findByProjectIdOrderByTimestampDesc(projectId))
            .thenReturn(mockMetrics);

        // When
        List<QualityMetrics> result = qualityMetricsService.getQualityHistory(projectId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(qualityMetrics1, result.get(0));
        assertEquals(qualityMetrics2, result.get(1));
        verify(qualityMetricsRepository).findByProjectIdOrderByTimestampDesc(projectId);
    }

    @Test
    public void testGetLatestQualityMetrics_Found() {
        // Given
        List<QualityMetrics> mockMetrics = Arrays.asList(qualityMetrics1);
        when(qualityMetricsRepository.findLatestByProject(projectId))
            .thenReturn(mockMetrics);

        // When
        Optional<QualityMetrics> result = qualityMetricsService.getLatestQualityMetrics(projectId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(qualityMetrics1, result.get());
        verify(qualityMetricsRepository).findLatestByProject(projectId);
    }

    @Test
    public void testGetLatestQualityMetrics_NotFound() {
        // Given
        when(qualityMetricsRepository.findLatestByProject(projectId))
            .thenReturn(Collections.emptyList());

        // When
        Optional<QualityMetrics> result = qualityMetricsService.getLatestQualityMetrics(projectId);

        // Then
        assertFalse(result.isPresent());
        verify(qualityMetricsRepository).findLatestByProject(projectId);
    }

    @Test
    public void testGetQualityMetricsByDateRange() {
        // Given
        List<QualityMetrics> mockMetrics = Arrays.asList(qualityMetrics1, qualityMetrics2);
        when(qualityMetricsRepository.findByProjectIdAndTimestampBetween(projectId, startDate, endDate))
            .thenReturn(mockMetrics);

        // When
        List<QualityMetrics> result = qualityMetricsService.getQualityMetricsByDateRange(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(qualityMetricsRepository).findByProjectIdAndTimestampBetween(projectId, startDate, endDate);
    }

    @Test
    public void testGetQualityTrend_WithData() {
        // Given
        Object[] mockResult = {5.0, 3.0, 80.0, 10.0};
        when(qualityMetricsRepository.getQualityTrend(projectId, startDate, endDate))
            .thenReturn(mockResult);

        // When
        QualityTrend result = qualityMetricsService.getQualityTrend(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(5.0, result.getAverageComplexity(), 0.1);
        assertEquals(3.0, result.getAverageDuplicateRate(), 0.1);
        assertEquals(80.0, result.getAverageMaintainabilityIndex(), 0.1);
        assertEquals(10.0, result.getAverageTechnicalDebt(), 0.1);
    }

    @Test
    public void testGetQualityTrend_NoData() {
        // Given
        when(qualityMetricsRepository.getQualityTrend(projectId, startDate, endDate))
            .thenReturn(null);

        // When
        QualityTrend result = qualityMetricsService.getQualityTrend(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(0.0, result.getAverageComplexity(), 0.1);
        assertEquals(0.0, result.getAverageDuplicateRate(), 0.1);
        assertEquals(0.0, result.getAverageMaintainabilityIndex(), 0.1);
        assertEquals(0.0, result.getAverageTechnicalDebt(), 0.1);
    }

    @Test
    public void testGetFailedQualityGates_WithProjectId() {
        // Given
        List<QualityMetrics> mockMetrics = Arrays.asList(qualityMetrics1);
        when(qualityMetricsRepository.findFailedQualityGatesByProject(projectId, startDate, endDate))
            .thenReturn(mockMetrics);

        // When
        List<QualityMetrics> result = qualityMetricsService.getFailedQualityGates(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(qualityMetricsRepository).findFailedQualityGatesByProject(projectId, startDate, endDate);
        verify(qualityMetricsRepository, never()).findFailedQualityGates(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    public void testGetFailedQualityGates_WithoutProjectId() {
        // Given
        List<QualityMetrics> mockMetrics = Arrays.asList(qualityMetrics1, qualityMetrics2);
        when(qualityMetricsRepository.findFailedQualityGates(startDate, endDate))
            .thenReturn(mockMetrics);

        // When
        List<QualityMetrics> result = qualityMetricsService.getFailedQualityGates(null, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(qualityMetricsRepository).findFailedQualityGates(startDate, endDate);
        verify(qualityMetricsRepository, never()).findFailedQualityGatesByProject(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    public void testGetQualityGateStats() {
        // Given
        List<Object[]> mockStats = Arrays.asList(
            new Object[]{"PASSED", 8L},
            new Object[]{"FAILED", 2L}
        );
        when(qualityMetricsRepository.getQualityGateStats(projectId, startDate, endDate))
            .thenReturn(mockStats);

        // When
        QualityGateStats result = qualityMetricsService.getQualityGateStats(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(10, result.getTotalGates().intValue());
        assertEquals(8, result.getPassedGates().intValue());
        assertEquals(2, result.getFailedGates().intValue());
        assertEquals(80.0, result.getPassRate(), 0.1);
    }

    @Test
    public void testGetQualityGateStats_NoData() {
        // Given
        when(qualityMetricsRepository.getQualityGateStats(projectId, startDate, endDate))
            .thenReturn(Collections.emptyList());

        // When
        QualityGateStats result = qualityMetricsService.getQualityGateStats(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalGates().intValue());
        assertEquals(0, result.getPassedGates().intValue());
        assertEquals(0, result.getFailedGates().intValue());
        assertEquals(0.0, result.getPassRate(), 0.1);
    }

    @Test
    public void testGetTechnicalDebtStats() {
        // Given
        Object[] mockResult = {100.0, 50.0, 80.0};
        when(qualityMetricsRepository.getTechnicalDebtStats(projectId, startDate, endDate))
            .thenReturn(mockResult);

        // When
        TechnicalDebtStats result = qualityMetricsService.getTechnicalDebtStats(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(100.0, result.getTotalDebt(), 0.1);
        assertEquals(50.0, result.getAverageDebt(), 0.1);
        assertEquals(80.0, result.getMaxDebt(), 0.1);
    }

    @Test
    public void testGetTechnicalDebtStats_NoData() {
        // Given
        when(qualityMetricsRepository.getTechnicalDebtStats(projectId, startDate, endDate))
            .thenReturn(null);

        // When
        TechnicalDebtStats result = qualityMetricsService.getTechnicalDebtStats(projectId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getTotalDebt(), 0.1);
        assertEquals(0.0, result.getAverageDebt(), 0.1);
        assertEquals(0.0, result.getMaxDebt(), 0.1);
    }

    @Test
    public void testCompareQualityBetweenPeriods() {
        // Given
        LocalDateTime period1Start = startDate.minusDays(30);
        LocalDateTime period1End = startDate;
        LocalDateTime period2Start = startDate;
        LocalDateTime period2End = endDate;
        
        Object[] mockResult = {5.0, 4.0, 3.0, 2.5, 80.0, 85.0};
        when(qualityMetricsRepository.compareQualityBetweenPeriods(
            projectId, period1Start, period1End, period2Start, period2End))
            .thenReturn(mockResult);

        // When
        QualityComparison result = qualityMetricsService.compareQualityBetweenPeriods(
            projectId, period1Start, period1End, period2Start, period2End);

        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(5.0, result.getPeriod1Complexity(), 0.1);
        assertEquals(4.0, result.getPeriod2Complexity(), 0.1);
        assertEquals(3.0, result.getPeriod1DuplicateRate(), 0.1);
        assertEquals(2.5, result.getPeriod2DuplicateRate(), 0.1);
        assertEquals(80.0, result.getPeriod1MaintainabilityIndex(), 0.1);
        assertEquals(85.0, result.getPeriod2MaintainabilityIndex(), 0.1);
        
        // 验证变化百分比计算
        assertEquals(-20.0, result.getComplexityChange(), 0.1); // (4-5)/5*100 = -20%
        assertEquals(-16.67, result.getDuplicateRateChange(), 0.1); // (2.5-3)/3*100 = -16.67%
        assertEquals(6.25, result.getMaintainabilityIndexChange(), 0.1); // (85-80)/80*100 = 6.25%
    }

    @Test
    public void testCompareQualityBetweenPeriods_NoData() {
        // Given
        LocalDateTime period1Start = startDate.minusDays(30);
        LocalDateTime period1End = startDate;
        LocalDateTime period2Start = startDate;
        LocalDateTime period2End = endDate;
        
        when(qualityMetricsRepository.compareQualityBetweenPeriods(
            projectId, period1Start, period1End, period2Start, period2End))
            .thenReturn(null);

        // When
        QualityComparison result = qualityMetricsService.compareQualityBetweenPeriods(
            projectId, period1Start, period1End, period2Start, period2End);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getPeriod1Complexity(), 0.1);
        assertEquals(0.0, result.getPeriod2Complexity(), 0.1);
        assertEquals(0.0, result.getComplexityChange(), 0.1);
    }

    @Test
    public void testGetAllProjectsQualityOverview() {
        // Given
        List<Object[]> mockResults = Arrays.asList(
            new Object[]{"project1", 10L, 5.0, 3.0, 80.0, 100.0},
            new Object[]{"project2", 8L, 4.0, 2.5, 85.0, 80.0}
        );
        when(qualityMetricsRepository.getAllProjectsQualityOverview(startDate, endDate))
            .thenReturn(mockResults);

        // When
        List<ProjectQualityOverview> result = qualityMetricsService.getAllProjectsQualityOverview(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        ProjectQualityOverview overview1 = result.get(0);
        assertEquals("project1", overview1.getProjectId());
        assertEquals(10, overview1.getTotalScans().intValue());
        assertEquals(5.0, overview1.getAverageComplexity(), 0.1);
        assertEquals(3.0, overview1.getAverageDuplicateRate(), 0.1);
        assertEquals(80.0, overview1.getAverageMaintainabilityIndex(), 0.1);
        assertEquals(100.0, overview1.getTotalTechnicalDebt(), 0.1);
        
        ProjectQualityOverview overview2 = result.get(1);
        assertEquals("project2", overview2.getProjectId());
        assertEquals(8, overview2.getTotalScans().intValue());
    }

    @Test
    public void testFindMostImprovedProjects() {
        // Given
        LocalDateTime oldStart = startDate.minusDays(60);
        LocalDateTime oldEnd = startDate.minusDays(30);
        LocalDateTime newStart = startDate;
        LocalDateTime newEnd = endDate;
        
        List<Object[]> mockResults = Arrays.asList(
            new Object[]{"project1", 70.0, 85.0},
            new Object[]{"project2", 75.0, 90.0}
        );
        when(qualityMetricsRepository.findMostImprovedProjects(oldStart, oldEnd, newStart, newEnd))
            .thenReturn(mockResults);

        // When
        List<ProjectImprovement> result = qualityMetricsService.findMostImprovedProjects(
            oldStart, oldEnd, newStart, newEnd);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        ProjectImprovement improvement1 = result.get(0);
        assertEquals("project1", improvement1.getProjectId());
        assertEquals(70.0, improvement1.getOldMaintainabilityIndex(), 0.1);
        assertEquals(85.0, improvement1.getNewMaintainabilityIndex(), 0.1);
        assertEquals(15.0, improvement1.getImprovement(), 0.1);
        
        ProjectImprovement improvement2 = result.get(1);
        assertEquals("project2", improvement2.getProjectId());
        assertEquals(15.0, improvement2.getImprovement(), 0.1);
    }

    @Test
    public void testCalculatePercentageChange_NormalCase() {
        // Given
        QualityComparison comparison = new QualityComparison();
        comparison.setPeriod1Complexity(10.0);
        comparison.setPeriod2Complexity(8.0);
        
        // When - 通过compareQualityBetweenPeriods间接测试
        Object[] mockResult = {10.0, 8.0, 0.0, 0.0, 0.0, 0.0};
        when(qualityMetricsRepository.compareQualityBetweenPeriods(
            anyString(), any(LocalDateTime.class), any(LocalDateTime.class), 
            any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockResult);
        
        QualityComparison result = qualityMetricsService.compareQualityBetweenPeriods(
            projectId, startDate, endDate, startDate, endDate);

        // Then
        assertEquals(-20.0, result.getComplexityChange(), 0.1); // (8-10)/10*100 = -20%
    }

    @Test
    public void testCalculatePercentageChange_ZeroOldValue() {
        // Given - 测试除零情况
        Object[] mockResult = {0.0, 8.0, 0.0, 0.0, 0.0, 0.0};
        when(qualityMetricsRepository.compareQualityBetweenPeriods(
            anyString(), any(LocalDateTime.class), any(LocalDateTime.class), 
            any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockResult);
        
        // When
        QualityComparison result = qualityMetricsService.compareQualityBetweenPeriods(
            projectId, startDate, endDate, startDate, endDate);

        // Then
        assertEquals(0.0, result.getComplexityChange(), 0.1); // 应该返回0而不是抛异常
    }

    @Test
    public void testDataClasses() {
        // Test QualityTrend constructor
        QualityTrend trend = new QualityTrend(projectId, startDate, endDate, 5.0, 3.0, 80.0, 10.0);
        assertEquals(projectId, trend.getProjectId());
        assertEquals(startDate, trend.getStartDate());
        assertEquals(endDate, trend.getEndDate());
        assertEquals(5.0, trend.getAverageComplexity(), 0.1);
        assertEquals(3.0, trend.getAverageDuplicateRate(), 0.1);
        assertEquals(80.0, trend.getAverageMaintainabilityIndex(), 0.1);
        assertEquals(10.0, trend.getAverageTechnicalDebt(), 0.1);

        // Test QualityGateStats
        QualityGateStats stats = new QualityGateStats();
        stats.setProjectId(projectId);
        stats.setTotalGates(10);
        stats.setPassedGates(8);
        stats.setFailedGates(2);
        stats.setPassRate(80.0);
        
        assertEquals(projectId, stats.getProjectId());
        assertEquals(10, stats.getTotalGates().intValue());
        assertEquals(8, stats.getPassedGates().intValue());
        assertEquals(2, stats.getFailedGates().intValue());
        assertEquals(80.0, stats.getPassRate(), 0.1);

        // Test TechnicalDebtStats
        TechnicalDebtStats debtStats = new TechnicalDebtStats();
        debtStats.setProjectId(projectId);
        debtStats.setTotalDebt(100.0);
        debtStats.setAverageDebt(50.0);
        debtStats.setMaxDebt(80.0);
        
        assertEquals(projectId, debtStats.getProjectId());
        assertEquals(100.0, debtStats.getTotalDebt(), 0.1);
        assertEquals(50.0, debtStats.getAverageDebt(), 0.1);
        assertEquals(80.0, debtStats.getMaxDebt(), 0.1);
    }

    // Helper methods

    private QualityMetrics createMockQualityMetrics(String id, Double complexity, Double duplicateRate, 
                                                   Double maintainabilityIndex, Double technicalDebt) {
        QualityMetrics metrics = new QualityMetrics();
        metrics.setId(Long.valueOf(id));
        metrics.setProjectId(projectId);
        metrics.setCommitSha("commit" + id);
        metrics.setTimestamp(LocalDateTime.now().minusDays(Long.valueOf(id)));
        metrics.setCodeComplexity(complexity);
        metrics.setDuplicateRate(duplicateRate);
        metrics.setMaintainabilityIndex(maintainabilityIndex);
        metrics.setTechnicalDebt(technicalDebt);
        return metrics;
    }
}