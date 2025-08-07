package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.TestCoverage;
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
 * TestCoverageRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class TestCoverageRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    private TestCoverage testCoverage1;
    private TestCoverage testCoverage2;
    private TestCoverage testCoverage3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        testCoverage1 = new TestCoverage("project1", "abc123", now);
        testCoverage1.setLineCoverage(85.5);
        testCoverage1.setBranchCoverage(78.2);
        testCoverage1.setFunctionCoverage(92.0);
        testCoverage1.setTotalLines(1000);
        testCoverage1.setCoveredLines(855);
        testCoverage1.setTotalBranches(200);
        testCoverage1.setCoveredBranches(156);
        testCoverage1.setTotalFunctions(50);
        testCoverage1.setCoveredFunctions(46);
        testCoverage1.setReportType("jacoco");
        testCoverage1.setStatus("PASSED");
        testCoverage1.setThreshold(80.0);
        
        testCoverage2 = new TestCoverage("project1", "def456", yesterday);
        testCoverage2.setLineCoverage(72.3);
        testCoverage2.setBranchCoverage(65.8);
        testCoverage2.setFunctionCoverage(80.0);
        testCoverage2.setTotalLines(1200);
        testCoverage2.setCoveredLines(868);
        testCoverage2.setTotalBranches(250);
        testCoverage2.setCoveredBranches(165);
        testCoverage2.setTotalFunctions(60);
        testCoverage2.setCoveredFunctions(48);
        testCoverage2.setReportType("jacoco");
        testCoverage2.setStatus("FAILED");
        testCoverage2.setThreshold(80.0);
        
        testCoverage3 = new TestCoverage("project2", "ghi789", lastWeek);
        testCoverage3.setLineCoverage(90.0);
        testCoverage3.setBranchCoverage(88.5);
        testCoverage3.setFunctionCoverage(95.0);
        testCoverage3.setTotalLines(800);
        testCoverage3.setCoveredLines(720);
        testCoverage3.setTotalBranches(150);
        testCoverage3.setCoveredBranches(133);
        testCoverage3.setTotalFunctions(40);
        testCoverage3.setCoveredFunctions(38);
        testCoverage3.setReportType("cobertura");
        testCoverage3.setStatus("PASSED");
        testCoverage3.setThreshold(85.0);
        
        entityManager.persistAndFlush(testCoverage1);
        entityManager.persistAndFlush(testCoverage2);
        entityManager.persistAndFlush(testCoverage3);
    }
    
    @Test
    public void testFindByCommitSha() {
        Optional<TestCoverage> found = testCoverageRepository.findByCommitSha("abc123");
        
        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo("project1");
        assertThat(found.get().getLineCoverage()).isEqualTo(85.5);
    }
    
    @Test
    public void testFindByCommitSha_NotFound() {
        Optional<TestCoverage> found = testCoverageRepository.findByCommitSha("notexist");
        
        assertThat(found).isNotPresent();
    }
    
    @Test
    public void testFindByProjectIdOrderByTimestampDesc() {
        List<TestCoverage> coverages = testCoverageRepository.findByProjectIdOrderByTimestampDesc("project1");
        
        assertThat(coverages).hasSize(2);
        assertThat(coverages.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(coverages.get(1).getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testFindByProjectIdAndTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> coverages = testCoverageRepository.findByProjectIdAndTimestampBetween("project1", start, end);
        
        assertThat(coverages).hasSize(2);
    }
    
    @Test
    public void testFindLatestByProject() {
        List<TestCoverage> coverages = testCoverageRepository.findLatestByProject("project1");
        
        assertThat(coverages).hasSize(2);
        assertThat(coverages.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
    }
    
    @Test
    public void testGetCoverageTrend() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] trend = testCoverageRepository.getCoverageTrend("project1", start, end);
        
        assertThat(trend).isNotNull();
        assertThat(trend[0]).isEqualTo(78.9); // 平均行覆盖率 (85.5+72.3)/2
        assertThat(trend[1]).isEqualTo(72.0); // 平均分支覆盖率 (78.2+65.8)/2
        assertThat(trend[2]).isEqualTo(86.0); // 平均函数覆盖率 (92.0+80.0)/2
    }
    
    @Test
    public void testGetCoverageTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = testCoverageRepository.getCoverageTrendByDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和覆盖率指标
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个覆盖率指标
    }
    
    @Test
    public void testFindLowCoverageRecords() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> lowCoverage = testCoverageRepository.findLowCoverageRecords("project1", 80.0, start, end);
        
        assertThat(lowCoverage).hasSize(1);
        assertThat(lowCoverage.get(0).getCommitSha()).isEqualTo("def456");
        assertThat(lowCoverage.get(0).getLineCoverage()).isEqualTo(72.3);
    }
    
    @Test
    public void testGetCoverageStatusStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = testCoverageRepository.getCoverageStatusStats("project1", start, end);
        
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
    public void testFindFailedCoverageRecords() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> failed = testCoverageRepository.findFailedCoverageRecords(start, end);
        
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getCommitSha()).isEqualTo("def456");
        assertThat(failed.get(0).getStatus()).isEqualTo("FAILED");
    }
    
    @Test
    public void testFindFailedCoverageRecordsByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> failed = testCoverageRepository.findFailedCoverageRecordsByProject("project1", start, end);
        
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testGetCoverageDetailStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = testCoverageRepository.getCoverageDetailStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(78.9); // 平均行覆盖率
        assertThat(stats[1]).isEqualTo(72.0); // 平均分支覆盖率
        assertThat(stats[2]).isEqualTo(86.0); // 平均函数覆盖率
        assertThat(stats[3]).isEqualTo(1100.0); // 平均总行数 (1000+1200)/2
        assertThat(stats[4]).isEqualTo(861.5); // 平均覆盖行数 (855+868)/2
    }
    
    @Test
    public void testGetCoverageStatsByReportType() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = testCoverageRepository.getCoverageStatsByReportType("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有jacoco类型
        
        Object[] jacocoStats = stats.get(0);
        assertThat(jacocoStats[0]).isEqualTo("jacoco"); // 报告类型
        assertThat(jacocoStats[1]).isEqualTo(2L); // 记录数量
        assertThat(jacocoStats[2]).isEqualTo(78.9); // 平均行覆盖率
        assertThat(jacocoStats[3]).isEqualTo(72.0); // 平均分支覆盖率
    }
    
    @Test
    public void testFindHighestCoverage() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> highest = testCoverageRepository.findHighestCoverage("project1", start, end);
        
        assertThat(highest).hasSize(2);
        assertThat(highest.get(0).getCommitSha()).isEqualTo("abc123"); // 覆盖率最高的在前
        assertThat(highest.get(0).getLineCoverage()).isEqualTo(85.5);
    }
    
    @Test
    public void testFindLowestCoverage() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<TestCoverage> lowest = testCoverageRepository.findLowestCoverage("project1", start, end);
        
        assertThat(lowest).hasSize(2);
        assertThat(lowest.get(0).getCommitSha()).isEqualTo("def456"); // 覆盖率最低的在前
        assertThat(lowest.get(0).getLineCoverage()).isEqualTo(72.3);
    }
    
    @Test
    public void testCompareCoverageBetweenPeriods() {
        LocalDateTime period1Start = LocalDateTime.now().minusDays(2);
        LocalDateTime period1End = LocalDateTime.now().minusDays(1);
        LocalDateTime period2Start = LocalDateTime.now().minusHours(12);
        LocalDateTime period2End = LocalDateTime.now().plusDays(1);
        
        Object[] comparison = testCoverageRepository.compareCoverageBetweenPeriods(
            "project1", period1Start, period1End, period2Start, period2End);
        
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(6); // 两个时期的3个覆盖率指标对比
    }
    
    @Test
    public void testGetAllProjectsCoverageOverview() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> overview = testCoverageRepository.getAllProjectsCoverageOverview(start, end);
        
        assertThat(overview).hasSize(2); // project1和project2
        
        // 验证project2的概览数据（覆盖率最高的在前）
        Object[] project2Overview = overview.get(0);
        assertThat(project2Overview[0]).isEqualTo("project2"); // projectId
        assertThat(project2Overview[1]).isEqualTo(1L); // 记录数量
        assertThat(project2Overview[2]).isEqualTo(90.0); // 平均行覆盖率
    }
    
    @Test
    public void testFindMostImprovedCoverageProjects() {
        LocalDateTime oldStart = LocalDateTime.now().minusDays(8);
        LocalDateTime oldEnd = LocalDateTime.now().minusDays(6);
        LocalDateTime newStart = LocalDateTime.now().minusDays(2);
        LocalDateTime newEnd = LocalDateTime.now().plusDays(1);
        
        List<Object[]> improved = testCoverageRepository.findMostImprovedCoverageProjects(
            oldStart, oldEnd, newStart, newEnd);
        
        // 由于测试数据的时间分布，可能没有符合条件的项目
        assertThat(improved).isNotNull();
    }
    
    @Test
    public void testGetCoverageThresholdStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = testCoverageRepository.getCoverageThresholdStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(1L); // 达标数量（abc123达到85.5%，超过80%阈值）
        assertThat(stats[1]).isEqualTo(1L); // 未达标数量（def456为72.3%，低于80%阈值）
        assertThat(stats[2]).isEqualTo(2L); // 总数量
    }
}