package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.TestCoverageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 覆盖率质量门禁服务
 * 负责检查测试覆盖率是否达到质量标准，并实施相应的质量门禁策略
 */
@Service
@Transactional
public class CoverageQualityGateService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoverageQualityGateService.class);
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    @Autowired
    private AlertService alertService;
    
    // 默认覆盖率阈值配置
    @Value("${coverage.threshold.line:80.0}")
    private Double defaultLineThreshold;
    
    @Value("${coverage.threshold.branch:70.0}")
    private Double defaultBranchThreshold;
    
    @Value("${coverage.threshold.function:80.0}")
    private Double defaultFunctionThreshold;
    
    @Value("${coverage.quality-gate.enabled:true}")
    private Boolean qualityGateEnabled;
    
    @Value("${coverage.quality-gate.strict-mode:false}")
    private Boolean strictMode;
    
    /**
     * 检查覆盖率质量门禁
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @return 质量门禁检查结果
     */
    public QualityGateResult checkQualityGate(String projectId, String commitSha) {
        logger.info("开始检查覆盖率质量门禁: projectId={}, commitSha={}", projectId, commitSha);
        
        if (!qualityGateEnabled) {
            logger.info("覆盖率质量门禁已禁用，跳过检查");
            return new QualityGateResult(true, "质量门禁已禁用", new ArrayList<>());
        }
        
        Optional<TestCoverage> coverageOpt = testCoverageRepository.findByCommitSha(commitSha);
        if (!coverageOpt.isPresent()) {
            logger.warn("未找到提交的覆盖率数据: commitSha={}", commitSha);
            return new QualityGateResult(false, "未找到覆盖率数据", 
                Arrays.asList("提交 " + commitSha + " 缺少测试覆盖率数据"));
        }
        
        TestCoverage coverage = coverageOpt.get();
        return evaluateCoverage(coverage);
    }
    
    /**
     * 检查覆盖率质量门禁（使用自定义阈值）
     */
    public QualityGateResult checkQualityGate(String projectId, String commitSha, 
                                            Double lineThreshold, Double branchThreshold, Double functionThreshold) {
        logger.info("开始检查覆盖率质量门禁（自定义阈值）: projectId={}, commitSha={}, lineThreshold={}, branchThreshold={}, functionThreshold={}", 
                   projectId, commitSha, lineThreshold, branchThreshold, functionThreshold);
        
        if (!qualityGateEnabled) {
            logger.info("覆盖率质量门禁已禁用，跳过检查");
            return new QualityGateResult(true, "质量门禁已禁用", new ArrayList<>());
        }
        
        Optional<TestCoverage> coverageOpt = testCoverageRepository.findByCommitSha(commitSha);
        if (!coverageOpt.isPresent()) {
            logger.warn("未找到提交的覆盖率数据: commitSha={}", commitSha);
            return new QualityGateResult(false, "未找到覆盖率数据", 
                Arrays.asList("提交 " + commitSha + " 缺少测试覆盖率数据"));
        }
        
        TestCoverage coverage = coverageOpt.get();
        return evaluateCoverage(coverage, lineThreshold, branchThreshold, functionThreshold);
    }
    
    /**
     * 评估覆盖率是否达标
     */
    private QualityGateResult evaluateCoverage(TestCoverage coverage) {
        return evaluateCoverage(coverage, defaultLineThreshold, defaultBranchThreshold, defaultFunctionThreshold);
    }
    
    /**
     * 评估覆盖率是否达标（使用指定阈值）
     */
    private QualityGateResult evaluateCoverage(TestCoverage coverage, 
                                             Double lineThreshold, Double branchThreshold, Double functionThreshold) {
        List<String> violations = new ArrayList<>();
        boolean passed = true;
        
        // 检查行覆盖率
        if (coverage.getLineCoverage() != null && lineThreshold != null) {
            if (coverage.getLineCoverage() < lineThreshold) {
                violations.add(String.format("行覆盖率 %.2f%% 低于阈值 %.2f%%", 
                    coverage.getLineCoverage(), lineThreshold));
                passed = false;
            }
        }
        
        // 检查分支覆盖率
        if (coverage.getBranchCoverage() != null && branchThreshold != null) {
            if (coverage.getBranchCoverage() < branchThreshold) {
                violations.add(String.format("分支覆盖率 %.2f%% 低于阈值 %.2f%%", 
                    coverage.getBranchCoverage(), branchThreshold));
                passed = false;
            }
        }
        
        // 检查函数覆盖率
        if (coverage.getFunctionCoverage() != null && functionThreshold != null) {
            if (coverage.getFunctionCoverage() < functionThreshold) {
                violations.add(String.format("函数覆盖率 %.2f%% 低于阈值 %.2f%%", 
                    coverage.getFunctionCoverage(), functionThreshold));
                passed = false;
            }
        }
        
        // 更新覆盖率状态
        String status = passed ? "PASSED" : "FAILED";
        coverage.setStatus(status);
        coverage.setThreshold(lineThreshold);
        testCoverageRepository.save(coverage);
        
        String message = passed ? "覆盖率质量门禁检查通过" : "覆盖率质量门禁检查失败";
        
        logger.info("覆盖率质量门禁检查完成: projectId={}, commitSha={}, passed={}, violations={}", 
                   coverage.getProjectId(), coverage.getCommitSha(), passed, violations.size());
        
        return new QualityGateResult(passed, message, violations);
    }
    
    /**
     * 检查新增代码的测试要求
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param newCodeLines 新增代码行数
     * @return 新增代码测试检查结果
     */
    public NewCodeTestResult checkNewCodeTestRequirement(String projectId, String commitSha, Integer newCodeLines) {
        logger.info("开始检查新增代码测试要求: projectId={}, commitSha={}, newCodeLines={}", 
                   projectId, commitSha, newCodeLines);
        
        if (newCodeLines == null || newCodeLines <= 0) {
            return new NewCodeTestResult(true, "无新增代码", 0, 0);
        }
        
        Optional<TestCoverage> coverageOpt = testCoverageRepository.findByCommitSha(commitSha);
        if (!coverageOpt.isPresent()) {
            logger.warn("未找到提交的覆盖率数据: commitSha={}", commitSha);
            return new NewCodeTestResult(false, "缺少覆盖率数据", newCodeLines, 0);
        }
        
        TestCoverage coverage = coverageOpt.get();
        
        // 获取历史覆盖率数据进行对比
        List<TestCoverage> historicalCoverages = testCoverageRepository
            .findByProjectIdOrderByTimestampDesc(projectId);
        
        if (historicalCoverages.size() < 2) {
            logger.info("历史覆盖率数据不足，无法进行新增代码检查");
            return new NewCodeTestResult(true, "历史数据不足", newCodeLines, 0);
        }
        
        TestCoverage previousCoverage = historicalCoverages.get(1); // 获取前一次的覆盖率
        
        // 计算新增代码的覆盖情况
        Integer currentCoveredLines = coverage.getCoveredLines();
        Integer previousCoveredLines = previousCoverage.getCoveredLines();
        
        if (currentCoveredLines == null || previousCoveredLines == null) {
            logger.warn("覆盖率数据不完整，无法计算新增代码覆盖率");
            return new NewCodeTestResult(false, "覆盖率数据不完整", newCodeLines, 0);
        }
        
        Integer newCoveredLines = Math.max(0, currentCoveredLines - previousCoveredLines);
        Double newCodeCoverage = newCodeLines > 0 ? (double) newCoveredLines / newCodeLines * 100 : 0.0;
        
        boolean passed = newCodeCoverage >= defaultLineThreshold;
        String message = passed ? 
            String.format("新增代码覆盖率 %.2f%% 达到要求", newCodeCoverage) :
            String.format("新增代码覆盖率 %.2f%% 低于阈值 %.2f%%", newCodeCoverage, defaultLineThreshold);
        
        logger.info("新增代码测试检查完成: projectId={}, commitSha={}, newCodeCoverage={}%, passed={}", 
                   projectId, commitSha, newCodeCoverage, passed);
        
        return new NewCodeTestResult(passed, message, newCodeLines, newCoveredLines);
    }
    
    /**
     * 阻止部署
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param reason 阻止原因
     */
    public void blockDeployment(String projectId, String commitSha, String reason) {
        logger.warn("阻止部署: projectId={}, commitSha={}, reason={}", projectId, commitSha, reason);
        
        // 发送告警通知
        alertService.sendCoverageAlert(projectId, commitSha, "DEPLOYMENT_BLOCKED", reason);
        
        // 记录部署阻止事件
        // 这里可以集成到CI/CD系统中，实际阻止部署流程
        logger.info("已记录部署阻止事件: projectId={}, commitSha={}", projectId, commitSha);
    }
    
    /**
     * 检查测试失败情况
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param testResults 测试结果
     * @return 测试失败检查结果
     */
    public TestFailureResult checkTestFailures(String projectId, String commitSha, TestResults testResults) {
        logger.info("开始检查测试失败情况: projectId={}, commitSha={}, totalTests={}, failedTests={}", 
                   projectId, commitSha, testResults.getTotalTests(), testResults.getFailedTests());
        
        boolean hasFailures = testResults.getFailedTests() > 0;
        
        if (hasFailures) {
            String reason = String.format("存在 %d 个失败的测试用例", testResults.getFailedTests());
            
            if (strictMode) {
                // 严格模式下，任何测试失败都阻止部署
                blockDeployment(projectId, commitSha, reason);
                return new TestFailureResult(false, reason, true);
            } else {
                // 非严格模式下，只记录警告
                logger.warn("检测到测试失败，但非严格模式下不阻止部署: {}", reason);
                return new TestFailureResult(false, reason, false);
            }
        }
        
        return new TestFailureResult(true, "所有测试通过", false);
    }
    
    /**
     * 获取项目的覆盖率质量门禁统计
     */
    @Transactional(readOnly = true)
    public QualityGateStats getQualityGateStats(String projectId, LocalDateTime start, LocalDateTime end) {
        Object[] stats = testCoverageRepository.getCoverageThresholdStats(projectId, start, end);
        
        if (stats != null && stats.length >= 3) {
            Long passed = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
            Long failed = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
            Long total = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
            
            return new QualityGateStats(projectId, start, end, passed, failed, total);
        }
        
        return new QualityGateStats(projectId, start, end, 0L, 0L, 0L);
    }
    
    /**
     * 质量门禁检查结果
     */
    public static class QualityGateResult {
        private boolean passed;
        private String message;
        private List<String> violations;
        
        public QualityGateResult(boolean passed, String message, List<String> violations) {
            this.passed = passed;
            this.message = message;
            this.violations = violations;
        }
        
        // Getters and Setters
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }
    }
    
    /**
     * 新增代码测试检查结果
     */
    public static class NewCodeTestResult {
        private boolean passed;
        private String message;
        private Integer newCodeLines;
        private Integer newCoveredLines;
        
        public NewCodeTestResult(boolean passed, String message, Integer newCodeLines, Integer newCoveredLines) {
            this.passed = passed;
            this.message = message;
            this.newCodeLines = newCodeLines;
            this.newCoveredLines = newCoveredLines;
        }
        
        // Getters and Setters
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getNewCodeLines() { return newCodeLines; }
        public void setNewCodeLines(Integer newCodeLines) { this.newCodeLines = newCodeLines; }
        
        public Integer getNewCoveredLines() { return newCoveredLines; }
        public void setNewCoveredLines(Integer newCoveredLines) { this.newCoveredLines = newCoveredLines; }
    }
    
    /**
     * 测试失败检查结果
     */
    public static class TestFailureResult {
        private boolean passed;
        private String message;
        private boolean deploymentBlocked;
        
        public TestFailureResult(boolean passed, String message, boolean deploymentBlocked) {
            this.passed = passed;
            this.message = message;
            this.deploymentBlocked = deploymentBlocked;
        }
        
        // Getters and Setters
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isDeploymentBlocked() { return deploymentBlocked; }
        public void setDeploymentBlocked(boolean deploymentBlocked) { this.deploymentBlocked = deploymentBlocked; }
    }
    
    /**
     * 测试结果数据
     */
    public static class TestResults {
        private Integer totalTests;
        private Integer passedTests;
        private Integer failedTests;
        private Integer skippedTests;
        
        public TestResults(Integer totalTests, Integer passedTests, Integer failedTests, Integer skippedTests) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
        }
        
        // Getters and Setters
        public Integer getTotalTests() { return totalTests; }
        public void setTotalTests(Integer totalTests) { this.totalTests = totalTests; }
        
        public Integer getPassedTests() { return passedTests; }
        public void setPassedTests(Integer passedTests) { this.passedTests = passedTests; }
        
        public Integer getFailedTests() { return failedTests; }
        public void setFailedTests(Integer failedTests) { this.failedTests = failedTests; }
        
        public Integer getSkippedTests() { return skippedTests; }
        public void setSkippedTests(Integer skippedTests) { this.skippedTests = skippedTests; }
    }
    
    /**
     * 质量门禁统计数据
     */
    public static class QualityGateStats {
        private String projectId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Long passedCount;
        private Long failedCount;
        private Long totalCount;
        
        public QualityGateStats(String projectId, LocalDateTime startDate, LocalDateTime endDate,
                              Long passedCount, Long failedCount, Long totalCount) {
            this.projectId = projectId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.totalCount = totalCount;
        }
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        
        public Long getPassedCount() { return passedCount; }
        public void setPassedCount(Long passedCount) { this.passedCount = passedCount; }
        
        public Long getFailedCount() { return failedCount; }
        public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
        
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        
        public Double getPassRate() {
            return totalCount > 0 ? (double) passedCount / totalCount * 100 : 0.0;
        }
    }
}