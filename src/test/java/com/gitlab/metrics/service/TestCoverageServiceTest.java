package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.TestCoverageRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TestCoverageService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class TestCoverageServiceTest {
    
    @Mock
    private TestCoverageRepository testCoverageRepository;
    
    @InjectMocks
    private TestCoverageService testCoverageService;
    
    private String projectId;
    private String commitSha;
    private LocalDateTime timestamp;
    
    @Before
    public void setUp() {
        projectId = "test-project";
        commitSha = "abc123def456";
        timestamp = LocalDateTime.now();
    }
    
    @Test
    public void testParseJaCoCoReport() {
        // 准备JaCoCo XML报告内容
        String jacocoReport = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<report name=\"Test Coverage Report\">\n" +
            "  <counter type=\"INSTRUCTION\" missed=\"100\" covered=\"900\"/>\n" +
            "  <counter type=\"LINE\" missed=\"20\" covered=\"80\"/>\n" +
            "  <counter type=\"BRANCH\" missed=\"10\" covered=\"40\"/>\n" +
            "  <counter type=\"METHOD\" missed=\"5\" covered=\"25\"/>\n" +
            "  <counter type=\"CLASS\" missed=\"2\" covered=\"8\"/>\n" +
            "</report>";
        
        when(testCoverageRepository.save(any(TestCoverage.class))).thenAnswer(invocation -> {
            TestCoverage coverage = invocation.getArgument(0);
            coverage.setId(1L);
            return coverage;
        });
        
        // 执行测试
        TestCoverage result = testCoverageService.parseCoverageReport(
            projectId, commitSha, jacocoReport, "jacoco", "/path/to/report.xml");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("jacoco", result.getReportType());
        assertEquals("/path/to/report.xml", result.getReportPath());
        assertEquals("PASSED", result.getStatus());
        
        // 验证覆盖率计算
        verify(testCoverageRepository).save(argThat(coverage -> {
            assertEquals(Integer.valueOf(100), coverage.getTotalLines());
            assertEquals(Integer.valueOf(80), coverage.getCoveredLines());
            assertEquals(Double.valueOf(80.0), coverage.getLineCoverage());
            
            assertEquals(Integer.valueOf(50), coverage.getTotalBranches());
            assertEquals(Integer.valueOf(40), coverage.getCoveredBranches());
            assertEquals(Double.valueOf(80.0), coverage.getBranchCoverage());
            
            assertEquals(Integer.valueOf(30), coverage.getTotalFunctions());
            assertEquals(Integer.valueOf(25), coverage.getCoveredFunctions());
            assertEquals(Double.valueOf(83.33333333333334), coverage.getFunctionCoverage());
            
            assertEquals(Integer.valueOf(10), coverage.getTotalClasses());
            assertEquals(Integer.valueOf(8), coverage.getCoveredClasses());
            
            return true;
        }));
    }
    
    @Test
    public void testParseCoberturaReport() {
        // 准备Cobertura XML报告内容
        String coberturaReport = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<coverage line-rate=\"0.85\" branch-rate=\"0.75\" version=\"1.9\">\n" +
            "  <packages>\n" +
            "    <package name=\"com.example\" line-rate=\"0.85\" branch-rate=\"0.75\">\n" +
            "      <classes>\n" +
            "        <class name=\"TestClass\" filename=\"TestClass.java\" line-rate=\"0.85\" branch-rate=\"0.75\">\n" +
            "          <lines>\n" +
            "            <line number=\"1\" hits=\"5\" branch=\"false\"/>\n" +
            "            <line number=\"2\" hits=\"0\" branch=\"false\"/>\n" +
            "            <line number=\"3\" hits=\"3\" branch=\"true\" condition-coverage=\"50% (1/2)\"/>\n" +
            "          </lines>\n" +
            "        </class>\n" +
            "      </classes>\n" +
            "    </package>\n" +
            "  </packages>\n" +
            "</coverage>";
        
        when(testCoverageRepository.save(any(TestCoverage.class))).thenAnswer(invocation -> {
            TestCoverage coverage = invocation.getArgument(0);
            coverage.setId(1L);
            return coverage;
        });
        
        // 执行测试
        TestCoverage result = testCoverageService.parseCoverageReport(
            projectId, commitSha, coberturaReport, "cobertura", "/path/to/coverage.xml");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("cobertura", result.getReportType());
        assertEquals("/path/to/coverage.xml", result.getReportPath());
        assertEquals("PASSED", result.getStatus());
        
        // 验证覆盖率计算
        verify(testCoverageRepository).save(argThat(coverage -> {
            assertEquals(Double.valueOf(85.0), coverage.getLineCoverage());
            assertEquals(Double.valueOf(75.0), coverage.getBranchCoverage());
            assertEquals(Integer.valueOf(3), coverage.getTotalLines());
            assertEquals(Integer.valueOf(2), coverage.getCoveredLines());
            assertEquals(Integer.valueOf(2), coverage.getTotalBranches());
            assertEquals(Integer.valueOf(1), coverage.getCoveredBranches());
            return true;
        }));
    }
    
    @Test
    public void testParseLcovReport() {
        // 准备LCOV报告内容
        String lcovReport = "TN:\n" +
            "SF:/path/to/file.js\n" +
            "FNF:10\n" +
            "FNH:8\n" +
            "LF:100\n" +
            "LH:85\n" +
            "BRF:20\n" +
            "BRH:15\n" +
            "end_of_record\n";
        
        when(testCoverageRepository.save(any(TestCoverage.class))).thenAnswer(invocation -> {
            TestCoverage coverage = invocation.getArgument(0);
            coverage.setId(1L);
            return coverage;
        });
        
        // 执行测试
        TestCoverage result = testCoverageService.parseCoverageReport(
            projectId, commitSha, lcovReport, "lcov", "/path/to/lcov.info");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("lcov", result.getReportType());
        assertEquals("/path/to/lcov.info", result.getReportPath());
        assertEquals("PASSED", result.getStatus());
        
        // 验证覆盖率计算
        verify(testCoverageRepository).save(argThat(coverage -> {
            assertEquals(Integer.valueOf(100), coverage.getTotalLines());
            assertEquals(Integer.valueOf(85), coverage.getCoveredLines());
            assertEquals(Double.valueOf(85.0), coverage.getLineCoverage());
            
            assertEquals(Integer.valueOf(10), coverage.getTotalFunctions());
            assertEquals(Integer.valueOf(8), coverage.getCoveredFunctions());
            assertEquals(Double.valueOf(80.0), coverage.getFunctionCoverage());
            
            assertEquals(Integer.valueOf(20), coverage.getTotalBranches());
            assertEquals(Integer.valueOf(15), coverage.getCoveredBranches());
            assertEquals(Double.valueOf(75.0), coverage.getBranchCoverage());
            
            return true;
        }));
    }
    
    @Test
    public void testParseUnsupportedReportType() {
        String reportContent = "unsupported content";
        
        // 执行测试并验证异常
        try {
            testCoverageService.parseCoverageReport(
                projectId, commitSha, reportContent, "unsupported", "/path/to/report");
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("解析测试覆盖率报告失败"));
        }
        
        // 验证保存了失败状态的记录
        verify(testCoverageRepository).save(argThat(coverage -> 
            "FAILED".equals(coverage.getStatus())
        ));
    }
    
    @Test
    public void testParseInvalidXmlReport() {
        String invalidXml = "invalid xml content";
        
        // 执行测试并验证异常
        try {
            testCoverageService.parseCoverageReport(
                projectId, commitSha, invalidXml, "jacoco", "/path/to/report.xml");
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("解析测试覆盖率报告失败"));
        }
        
        // 验证保存了失败状态的记录
        verify(testCoverageRepository).save(argThat(coverage -> 
            "FAILED".equals(coverage.getStatus())
        ));
    }
    
    @Test
    public void testGetCoverageHistory() {
        // 准备测试数据
        TestCoverage coverage1 = new TestCoverage(projectId, "commit1", timestamp.minusDays(1));
        TestCoverage coverage2 = new TestCoverage(projectId, "commit2", timestamp);
        List<TestCoverage> expectedCoverages = Arrays.asList(coverage2, coverage1);
        
        when(testCoverageRepository.findByProjectIdOrderByTimestampDesc(projectId))
            .thenReturn(expectedCoverages);
        
        // 执行测试
        List<TestCoverage> result = testCoverageService.getCoverageHistory(projectId);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(coverage2, result.get(0));
        assertEquals(coverage1, result.get(1));
        
        verify(testCoverageRepository).findByProjectIdOrderByTimestampDesc(projectId);
    }
    
    @Test
    public void testGetLatestCoverage() {
        // 准备测试数据
        TestCoverage latestCoverage = new TestCoverage(projectId, commitSha, timestamp);
        when(testCoverageRepository.findLatestByProject(projectId))
            .thenReturn(Arrays.asList(latestCoverage));
        
        // 执行测试
        Optional<TestCoverage> result = testCoverageService.getLatestCoverage(projectId);
        
        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(latestCoverage, result.get());
        
        verify(testCoverageRepository).findLatestByProject(projectId);
    }
    
    @Test
    public void testGetLatestCoverageEmpty() {
        // 准备测试数据
        when(testCoverageRepository.findLatestByProject(projectId))
            .thenReturn(Arrays.asList());
        
        // 执行测试
        Optional<TestCoverage> result = testCoverageService.getLatestCoverage(projectId);
        
        // 验证结果
        assertFalse(result.isPresent());
        
        verify(testCoverageRepository).findLatestByProject(projectId);
    }
    
    @Test
    public void testGetCoverageByDateRange() {
        // 准备测试数据
        LocalDateTime start = timestamp.minusDays(7);
        LocalDateTime end = timestamp;
        TestCoverage coverage = new TestCoverage(projectId, commitSha, timestamp);
        List<TestCoverage> expectedCoverages = Arrays.asList(coverage);
        
        when(testCoverageRepository.findByProjectIdAndTimestampBetween(projectId, start, end))
            .thenReturn(expectedCoverages);
        
        // 执行测试
        List<TestCoverage> result = testCoverageService.getCoverageByDateRange(projectId, start, end);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(coverage, result.get(0));
        
        verify(testCoverageRepository).findByProjectIdAndTimestampBetween(projectId, start, end);
    }
    
    @Test
    public void testGetCoverageTrend() {
        // 准备测试数据
        LocalDateTime start = timestamp.minusDays(7);
        LocalDateTime end = timestamp;
        Object[] trendData = {85.5, 75.2, 90.1};
        
        when(testCoverageRepository.getCoverageTrend(projectId, start, end))
            .thenReturn(trendData);
        
        // 执行测试
        TestCoverageService.CoverageTrend result = testCoverageService.getCoverageTrend(projectId, start, end);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(Double.valueOf(85.5), result.getAverageLineCoverage());
        assertEquals(Double.valueOf(75.2), result.getAverageBranchCoverage());
        assertEquals(Double.valueOf(90.1), result.getAverageFunctionCoverage());
        
        verify(testCoverageRepository).getCoverageTrend(projectId, start, end);
    }
    
    @Test
    public void testGetCoverageTrendEmpty() {
        // 准备测试数据
        LocalDateTime start = timestamp.minusDays(7);
        LocalDateTime end = timestamp;
        
        when(testCoverageRepository.getCoverageTrend(projectId, start, end))
            .thenReturn(null);
        
        // 执行测试
        TestCoverageService.CoverageTrend result = testCoverageService.getCoverageTrend(projectId, start, end);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(Double.valueOf(0.0), result.getAverageLineCoverage());
        assertEquals(Double.valueOf(0.0), result.getAverageBranchCoverage());
        assertEquals(Double.valueOf(0.0), result.getAverageFunctionCoverage());
        
        verify(testCoverageRepository).getCoverageTrend(projectId, start, end);
    }
    
    @Test
    public void testCleanupOldCoverageRecords() {
        // 准备测试数据
        LocalDateTime cutoffDate = timestamp.minusDays(30);
        TestCoverage oldCoverage1 = new TestCoverage(projectId, "old1", timestamp.minusDays(35));
        TestCoverage oldCoverage2 = new TestCoverage(projectId, "old2", timestamp.minusDays(40));
        TestCoverage newCoverage = new TestCoverage(projectId, "new", timestamp.minusDays(10));
        
        List<TestCoverage> allCoverages = Arrays.asList(oldCoverage1, oldCoverage2, newCoverage);
        when(testCoverageRepository.findAll()).thenReturn(allCoverages);
        
        // 执行测试
        testCoverageService.cleanupOldCoverageRecords(cutoffDate);
        
        // 验证结果
        verify(testCoverageRepository).findAll();
        verify(testCoverageRepository).deleteAll(argThat(coverages -> {
            List<TestCoverage> coverageList = (List<TestCoverage>) coverages;
            return coverageList.size() == 2 && 
                   coverageList.contains(oldCoverage1) && 
                   coverageList.contains(oldCoverage2);
        }));
    }
    
    @Test
    public void testCleanupOldCoverageRecordsEmpty() {
        // 准备测试数据
        LocalDateTime cutoffDate = timestamp.minusDays(30);
        TestCoverage newCoverage = new TestCoverage(projectId, "new", timestamp.minusDays(10));
        
        List<TestCoverage> allCoverages = Arrays.asList(newCoverage);
        when(testCoverageRepository.findAll()).thenReturn(allCoverages);
        
        // 执行测试
        testCoverageService.cleanupOldCoverageRecords(cutoffDate);
        
        // 验证结果
        verify(testCoverageRepository).findAll();
        verify(testCoverageRepository, never()).deleteAll(any());
    }
}