package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.TestCoverageRepository;
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
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CoverageQualityGateService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class CoverageQualityGateServiceTest {
    
    @Mock
    private TestCoverageRepository testCoverageRepository;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private CoverageQualityGateService qualityGateService;
    
    private String projectId;
    private String commitSha;
    private LocalDateTime timestamp;
    private TestCoverage testCoverage;
    
    @Before
    public void setUp() {
        projectId = "test-project";
        commitSha = "abc123def456";
        timestamp = LocalDateTime.now();
        
        // 设置默认配置值
        ReflectionTestUtils.setField(qualityGateService, "defaultLineThreshold", 80.0);
        ReflectionTestUtils.setField(qualityGateService, "defaultBranchThreshold", 70.0);
        ReflectionTestUtils.setField(qualityGateService, "defaultFunctionThreshold", 80.0);
        ReflectionTestUtils.setField(qualityGateService, "qualityGateEnabled", true);
        ReflectionTestUtils.setField(qualityGateService, "strictMode", false);
        
        // 创建测试覆盖率数据
        testCoverage = new TestCoverage(projectId, commitSha, timestamp);
        testCoverage.setId(1L);
        testCoverage.setLineCoverage(85.0);
        testCoverage.setBranchCoverage(75.0);
        testCoverage.setFunctionCoverage(90.0);
        testCoverage.setTotalLines(100);
        testCoverage.setCoveredLines(85);
    }
    
    @Test
    public void testCheckQualityGatePass() {
        // 准备测试数据
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.save(any(TestCoverage.class))).thenReturn(testCoverage);
        
        // 执行测试
        CoverageQualityGateService.QualityGateResult result = 
            qualityGateService.checkQualityGate(projectId, commitSha);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals("覆盖率质量门禁检查通过", result.getMessage());
        assertTrue(result.getViolations().isEmpty());
        
        // 验证覆盖率状态被更新为PASSED
        verify(testCoverageRepository).save(argThat(coverage -> 
            "PASSED".equals(coverage.getStatus())
        ));
    }
    
    @Test
    public void testCheckQualityGateFail() {
        // 准备测试数据 - 覆盖率低于阈值
        testCoverage.setLineCoverage(70.0);  // 低于80%阈值
        testCoverage.setBranchCoverage(60.0); // 低于70%阈值
        
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.save(any(TestCoverage.class))).thenReturn(testCoverage);
        
        // 执行测试
        CoverageQualityGateService.QualityGateResult result = 
            qualityGateService.checkQualityGate(projectId, commitSha);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertEquals("覆盖率质量门禁检查失败", result.getMessage());
        assertEquals(2, result.getViolations().size());
        assertTrue(result.getViolations().get(0).contains("行覆盖率 70.00% 低于阈值 80.00%"));
        assertTrue(result.getViolations().get(1).contains("分支覆盖率 60.00% 低于阈值 70.00%"));
        
        // 验证覆盖率状态被更新为FAILED
        verify(testCoverageRepository).save(argThat(coverage -> 
            "FAILED".equals(coverage.getStatus())
        ));
    }
    
    @Test
    public void testCheckQualityGateWithCustomThreshold() {
        // 准备测试数据
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.save(any(TestCoverage.class))).thenReturn(testCoverage);
        
        // 执行测试 - 使用更高的自定义阈值
        CoverageQualityGateService.QualityGateResult result = 
            qualityGateService.checkQualityGate(projectId, commitSha, 90.0, 80.0, 95.0);
        
        // 验证结果 - 应该失败，因为阈值更高
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertEquals("覆盖率质量门禁检查失败", result.getMessage());
        assertEquals(3, result.getViolations().size());
        assertTrue(result.getViolations().get(0).contains("行覆盖率 85.00% 低于阈值 90.00%"));
        assertTrue(result.getViolations().get(1).contains("分支覆盖率 75.00% 低于阈值 80.00%"));
        assertTrue(result.getViolations().get(2).contains("函数覆盖率 90.00% 低于阈值 95.00%"));
    }
    
    @Test
    public void testCheckQualityGateDisabled() {
        // 禁用质量门禁
        ReflectionTestUtils.setField(qualityGateService, "qualityGateEnabled", false);
        
        // 执行测试
        CoverageQualityGateService.QualityGateResult result = 
            qualityGateService.checkQualityGate(projectId, commitSha);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals("质量门禁已禁用", result.getMessage());
        assertTrue(result.getViolations().isEmpty());
        
        // 验证没有查询数据库
        verify(testCoverageRepository, never()).findByCommitSha(any());
    }
    
    @Test
    public void testCheckQualityGateNoCoverageData() {
        // 准备测试数据 - 没有覆盖率数据
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.empty());
        
        // 执行测试
        CoverageQualityGateService.QualityGateResult result = 
            qualityGateService.checkQualityGate(projectId, commitSha);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertEquals("未找到覆盖率数据", result.getMessage());
        assertEquals(1, result.getViolations().size());
        assertTrue(result.getViolations().get(0).contains("缺少测试覆盖率数据"));
    }
    
    @Test
    public void testCheckNewCodeTestRequirementPass() {
        // 准备测试数据
        TestCoverage previousCoverage = new TestCoverage(projectId, "previous-commit", timestamp.minusDays(1));
        previousCoverage.setCoveredLines(70);
        
        testCoverage.setCoveredLines(85); // 新增15行覆盖
        
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.findByProjectIdOrderByTimestampDesc(projectId))
            .thenReturn(Arrays.asList(testCoverage, previousCoverage));
        
        // 执行测试 - 新增15行代码
        CoverageQualityGateService.NewCodeTestResult result = 
            qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 15);
        
        // 验证结果 - 新增代码覆盖率100%，应该通过
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertTrue(result.getMessage().contains("新增代码覆盖率 100.00% 达到要求"));
        assertEquals(Integer.valueOf(15), result.getNewCodeLines());
        assertEquals(Integer.valueOf(15), result.getNewCoveredLines());
    }
    
    @Test
    public void testCheckNewCodeTestRequirementFail() {
        // 准备测试数据
        TestCoverage previousCoverage = new TestCoverage(projectId, "previous-commit", timestamp.minusDays(1));
        previousCoverage.setCoveredLines(70);
        
        testCoverage.setCoveredLines(80); // 新增10行覆盖
        
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.findByProjectIdOrderByTimestampDesc(projectId))
            .thenReturn(Arrays.asList(testCoverage, previousCoverage));
        
        // 执行测试 - 新增20行代码，但只覆盖了10行
        CoverageQualityGateService.NewCodeTestResult result = 
            qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 20);
        
        // 验证结果 - 新增代码覆盖率50%，应该失败
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("新增代码覆盖率 50.00% 低于阈值 80.00%"));
        assertEquals(Integer.valueOf(20), result.getNewCodeLines());
        assertEquals(Integer.valueOf(10), result.getNewCoveredLines());
    }
    
    @Test
    public void testCheckNewCodeTestRequirementNoNewCode() {
        // 执行测试 - 没有新增代码
        CoverageQualityGateService.NewCodeTestResult result = 
            qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 0);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals("无新增代码", result.getMessage());
        assertEquals(Integer.valueOf(0), result.getNewCodeLines());
        assertEquals(Integer.valueOf(0), result.getNewCoveredLines());
    }
    
    @Test
    public void testCheckNewCodeTestRequirementInsufficientHistory() {
        // 准备测试数据 - 只有一条历史记录
        when(testCoverageRepository.findByCommitSha(commitSha)).thenReturn(Optional.of(testCoverage));
        when(testCoverageRepository.findByProjectIdOrderByTimestampDesc(projectId))
            .thenReturn(Arrays.asList(testCoverage));
        
        // 执行测试
        CoverageQualityGateService.NewCodeTestResult result = 
            qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 10);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals("历史数据不足", result.getMessage());
        assertEquals(Integer.valueOf(10), result.getNewCodeLines());
        assertEquals(Integer.valueOf(0), result.getNewCoveredLines());
    }
    
    @Test
    public void testCheckTestFailuresPass() {
        // 准备测试数据 - 所有测试通过
        CoverageQualityGateService.TestResults testResults = 
            new CoverageQualityGateService.TestResults(100, 100, 0, 0);
        
        // 执行测试
        CoverageQualityGateService.TestFailureResult result = 
            qualityGateService.checkTestFailures(projectId, commitSha, testResults);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals("所有测试通过", result.getMessage());
        assertFalse(result.isDeploymentBlocked());
        
        // 验证没有发送告警
        verify(alertService, never()).sendCoverageAlert(any(), any(), any(), any());
    }
    
    @Test
    public void testCheckTestFailuresFailNonStrictMode() {
        // 准备测试数据 - 有测试失败，非严格模式
        CoverageQualityGateService.TestResults testResults = 
            new CoverageQualityGateService.TestResults(100, 95, 5, 0);
        
        // 执行测试
        CoverageQualityGateService.TestFailureResult result = 
            qualityGateService.checkTestFailures(projectId, commitSha, testResults);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("存在 5 个失败的测试用例"));
        assertFalse(result.isDeploymentBlocked()); // 非严格模式不阻止部署
        
        // 验证没有发送告警（非严格模式）
        verify(alertService, never()).sendCoverageAlert(any(), any(), any(), any());
    }
    
    @Test
    public void testCheckTestFailuresFailStrictMode() {
        // 设置严格模式
        ReflectionTestUtils.setField(qualityGateService, "strictMode", true);
        
        // 准备测试数据 - 有测试失败，严格模式
        CoverageQualityGateService.TestResults testResults = 
            new CoverageQualityGateService.TestResults(100, 95, 5, 0);
        
        // 执行测试
        CoverageQualityGateService.TestFailureResult result = 
            qualityGateService.checkTestFailures(projectId, commitSha, testResults);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("存在 5 个失败的测试用例"));
        assertTrue(result.isDeploymentBlocked()); // 严格模式阻止部署
        
        // 验证发送了告警
        verify(alertService).sendCoverageAlert(eq(projectId), eq(commitSha), eq("DEPLOYMENT_BLOCKED"), 
            argThat(reason -> reason.contains("存在 5 个失败的测试用例")));
    }
    
    @Test
    public void testBlockDeployment() {
        String reason = "覆盖率不达标";
        
        // 执行测试
        qualityGateService.blockDeployment(projectId, commitSha, reason);
        
        // 验证发送了告警
        verify(alertService).sendCoverageAlert(projectId, commitSha, "DEPLOYMENT_BLOCKED", reason);
    }
    
    @Test
    public void testGetQualityGateStats() {
        // 准备测试数据
        LocalDateTime start = timestamp.minusDays(7);
        LocalDateTime end = timestamp;
        Object[] statsData = {80L, 20L, 100L}; // passed, failed, total
        
        when(testCoverageRepository.getCoverageThresholdStats(projectId, start, end))
            .thenReturn(statsData);
        
        // 执行测试
        CoverageQualityGateService.QualityGateStats result = 
            qualityGateService.getQualityGateStats(projectId, start, end);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(Long.valueOf(80L), result.getPassedCount());
        assertEquals(Long.valueOf(20L), result.getFailedCount());
        assertEquals(Long.valueOf(100L), result.getTotalCount());
        assertEquals(Double.valueOf(80.0), result.getPassRate());
    }
    
    @Test
    public void testGetQualityGateStatsEmpty() {
        // 准备测试数据 - 没有统计数据
        LocalDateTime start = timestamp.minusDays(7);
        LocalDateTime end = timestamp;
        
        when(testCoverageRepository.getCoverageThresholdStats(projectId, start, end))
            .thenReturn(null);
        
        // 执行测试
        CoverageQualityGateService.QualityGateStats result = 
            qualityGateService.getQualityGateStats(projectId, start, end);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(Long.valueOf(0L), result.getPassedCount());
        assertEquals(Long.valueOf(0L), result.getFailedCount());
        assertEquals(Long.valueOf(0L), result.getTotalCount());
        assertEquals(Double.valueOf(0.0), result.getPassRate());
    }
}