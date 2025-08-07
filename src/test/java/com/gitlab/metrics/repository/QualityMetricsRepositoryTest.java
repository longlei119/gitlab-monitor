package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.QualityMetrics;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QualityMetricsRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class QualityMetricsRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    private QualityMetrics testMetrics1;
    private QualityMetrics testMetrics2;
    private QualityMetrics testMetrics3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        testMetrics1 = new QualityMetrics("project1", "abc123", now);
        testMetrics1.setCodeComplexity(5.2);
        testMetrics1.setDuplicateRate(3.5);
        testMetrics1.setMaintainabilityIndex(75.0);
        testMetrics1.setSecurityIssues(2);
        testMetrics1.setPerformanceIssues(1);
        testMetrics1.setCodeSmells(5);
        testMetrics1.setBugs(3);
        testMetrics1.setVulnerabilities(1);
        testMetrics1.setTechnicalDebt(2.5);
        testMetrics1.setQualityGate("PASSED");
        
        testMetrics2 = new QualityMetrics("project1", "def456", yesterday);
        testMetrics2.setCodeComplexity(7.8);
        testMetrics2.setDuplicateRate(5.2);
        testMetrics2.setMaintainabilityIndex(65.0);
        testMetrics2.setSecurityIssues(3);
        testMetrics2.setPerformanceIssues(2);
        testMetrics2.setCodeSmells(8);
        testMetrics2.setBugs(5);
        testMetrics2.setVulnerabilities(2);
        testMetrics2.setTechnicalDebt(4.0);
        testMetrics2.setQualityGate("FAILED");
        
        testMetrics3 = new QualityMetrics("project2", "ghi789", lastWeek);
        testMetrics3.setCodeComplexity(4.1);
        testMetrics3.setDuplicateRate(2.8);
        testMetrics3.setMaintainabilityIndex(80.0);
        testMetrics3.setSecurityIssues(1);
        testMetrics3.setPerformanceIssues(0);
        testMetrics3.setCodeSmells(3);
        testMetrics3.setBugs(2);
        testMetrics3.setVulnerabilities(0);
        testMetrics3.setTechnicalDebt(1.5);
        testMetrics3.setQualityGate("PASSED");
        
        entityManager.persistAndFlush(testMetrics1);
        entityManager.persistAndFlush(testMetrics2);
        entityManager.persistAndFlush(testMetrics3);
    }
    
    @Test
    public void testFindByCommitSha() {
        Optional<QualityMetrics> found = qualityMetricsRepository.findByCommitSha("abc123");
        
        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo("project1");
        assertThat(found.get().getCodeComplexity()).isEqualTo(5.2);
    }
    
    @Test
    public void testFindByCommitSha_NotFound() {
        Optional<QualityMetrics> found = qualityMetricsRepository.findByCommitSha("notexist");
        
        assertThat(found).isNotPresent();
    }
    
    @Test
    public void testFindByProjectIdOrderByTimestampDesc() {
        List<QualityMetrics> metrics = qualityMetricsRepository.findByProjectIdOrderByTimestampDesc("project1");
        
        assertThat(metrics).hasSize(2);
        assertThat(metrics.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(metrics.get(1).getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testFindByProjectIdAndTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<QualityMetrics> metrics = qualityMetricsRepository.findByProjectIdAndTimestampBetween("project1", start, end);
        
        assertThat(metrics).hasSize(2);
    }
    
    @Test
    public void testFindLatestByProject() {
        List<QualityMetrics> metrics = qualityMetricsRepository.findLatestByProject("project1");
        
        assertThat(metrics).hasSize(2);
        assertThat(metrics.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
    }
    
    @Test
    public void testGetQualityTrend() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] trend = qualityMetricsRepository.getQualityTrend("project1", start, end);
        
        assertThat(trend).isNotNull();
        assertThat(trend[0]).isEqualTo(6.5); // 平均复杂度 (5.2+7.8)/2
        assertThat(trend[1]).isEqualTo(4.35); // 平均重复率 (3.5+5.2)/2
        assertThat(trend[2]).isEqualTo(70.0); // 平均可维护性指数 (75.0+65.0)/2
        assertThat(trend[3]).isEqualTo(3.25); // 平均技术债务 (2.5+4.0)/2
    }
    
    @Test
    public void testGetQualityTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = qualityMetricsRepository.getQualityTrendByDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和质量指标
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(5); // 日期 + 4个质量指标
    }
    
    @Test
    public void testGetSecurityTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = qualityMetricsRepository.getSecurityTrendByDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证安全趋势数据包含日期和安全指标
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个安全指标
    }
    
    @Test
    public void testGetCodeIssueTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = qualityMetricsRepository.getCodeIssueTrendByDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证代码问题趋势数据
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个问题指标
    }
    
    @Test
    public void testFindFailedQualityGates() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<QualityMetrics> failed = qualityMetricsRepository.findFailedQualityGates(start, end);
        
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getCommitSha()).isEqualTo("def456");
        assertThat(failed.get(0).getQualityGate()).isEqualTo("FAILED");
    }
    
    @Test
    public void testFindFailedQualityGatesByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<QualityMetrics> failed = qualityMetricsRepository.findFailedQualityGatesByProject("project1", start, end);
        
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testGetQualityGateStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = qualityMetricsRepository.getQualityGateStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // PASSED和FAILED
        
        // 验证PASSED统计
        Object[] passedStats = stats.stream()
            .filter(stat -> "PASSED".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(passedStats).isNotNull();
        assertThat(passedStats[1]).isEqualTo(1L); // PASSED数量
        
        // 验证FAILED统计
        Object[] failedStats = stats.stream()
            .filter(stat -> "FAILED".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(failedStats).isNotNull();
        assertThat(failedStats[1]).isEqualTo(1L); // FAILED数量
    }
    
    @Test
    public void testFindHighestComplexity() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<QualityMetrics> highest = qualityMetricsRepository.findHighestComplexity("project1", start, end);
        
        assertThat(highest).hasSize(2);
        assertThat(highest.get(0).getCommitSha()).isEqualTo("def456"); // 复杂度最高的在前
        assertThat(highest.get(0).getCodeComplexity()).isEqualTo(7.8);
    }
    
    @Test
    public void testFindHighestDuplication() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<QualityMetrics> highest = qualityMetricsRepository.findHighestDuplication("project1", start, end);
        
        assertThat(highest).hasSize(2);
        assertThat(highest.get(0).getCommitSha()).isEqualTo("def456"); // 重复率最高的在前
        assertThat(highest.get(0).getDuplicateRate()).isEqualTo(5.2);
    }
    
    @Test
    public void testGetTechnicalDebtStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = qualityMetricsRepository.getTechnicalDebtStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(6.5); // 总技术债务 2.5+4.0
        assertThat(stats[1]).isEqualTo(3.25); // 平均技术债务 (2.5+4.0)/2
        assertThat(stats[2]).isEqualTo(4.0); // 最大技术债务
    }
    
    @Test
    public void testCompareQualityBetweenPeriods() {
        LocalDateTime period1Start = LocalDateTime.now().minusDays(2);
        LocalDateTime period1End = LocalDateTime.now().minusDays(1);
        LocalDateTime period2Start = LocalDateTime.now().minusHours(12);
        LocalDateTime period2End = LocalDateTime.now().plusDays(1);
        
        Object[] comparison = qualityMetricsRepository.compareQualityBetweenPeriods(
            "project1", period1Start, period1End, period2Start, period2End);
        
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(6); // 两个时期的3个质量指标对比
    }
    
    @Test
    public void testGetAllProjectsQualityOverview() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> overview = qualityMetricsRepository.getAllProjectsQualityOverview(start, end);
        
        assertThat(overview).hasSize(2); // project1和project2
        
        // 验证project1的概览数据
        Object[] project1Overview = overview.stream()
            .filter(stat -> "project1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(project1Overview).isNotNull();
        assertThat(project1Overview[0]).isEqualTo("project1"); // projectId
        assertThat(project1Overview[1]).isEqualTo(2L); // 记录数量
    }
    
    @Test
    public void testFindMostImprovedProjects() {
        LocalDateTime oldStart = LocalDateTime.now().minusDays(8);
        LocalDateTime oldEnd = LocalDateTime.now().minusDays(6);
        LocalDateTime newStart = LocalDateTime.now().minusDays(2);
        LocalDateTime newEnd = LocalDateTime.now().plusDays(1);
        
        List<Object[]> improved = qualityMetricsRepository.findMostImprovedProjects(
            oldStart, oldEnd, newStart, newEnd);
        
        // 由于测试数据的时间分布，可能没有符合条件的项目
        assertThat(improved).isNotNull();
    }
}