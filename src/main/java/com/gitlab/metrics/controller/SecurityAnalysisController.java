package com.gitlab.metrics.controller;

import com.gitlab.metrics.service.SecurityAnalysisService;
import com.gitlab.metrics.service.SecurityAnalysisService.SecurityAnalysisResult;
import com.gitlab.metrics.service.SecurityAnalysisService.PerformanceAnalysisResult;
import com.gitlab.metrics.service.SecurityAnalysisService.QualityThresholdResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 安全分析控制器
 * 提供安全和性能分析的REST API接口
 */
@RestController
@RequestMapping("/api/security")
public class SecurityAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAnalysisController.class);
    
    @Autowired
    private SecurityAnalysisService securityAnalysisService;
    
    /**
     * 分析项目安全漏洞
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 安全分析结果
     */
    @PostMapping("/analyze/vulnerabilities")
    public ResponseEntity<SecurityAnalysisResult> analyzeSecurityVulnerabilities(
            @RequestParam String projectId,
            @RequestParam String sonarProjectKey) {
        
        try {
            logger.info("接收安全漏洞分析请求: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            SecurityAnalysisResult result = securityAnalysisService.analyzeSecurityVulnerabilities(projectId, sonarProjectKey);
            
            logger.info("成功完成安全漏洞分析: projectId={}, 漏洞数量={}", projectId, result.getTotalVulnerabilities());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("安全漏洞分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 分析项目性能问题
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 性能分析结果
     */
    @PostMapping("/analyze/performance")
    public ResponseEntity<PerformanceAnalysisResult> analyzePerformanceIssues(
            @RequestParam String projectId,
            @RequestParam String sonarProjectKey) {
        
        try {
            logger.info("接收性能问题分析请求: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            PerformanceAnalysisResult result = securityAnalysisService.analyzePerformanceIssues(projectId, sonarProjectKey);
            
            logger.info("成功完成性能问题分析: projectId={}, 问题数量={}", projectId, result.getTotalIssues());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("性能问题分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查质量阈值
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 质量阈值检查结果
     */
    @PostMapping("/check/thresholds")
    public ResponseEntity<QualityThresholdResult> checkQualityThresholds(
            @RequestParam String projectId,
            @RequestParam String sonarProjectKey) {
        
        try {
            logger.info("接收质量阈值检查请求: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            QualityThresholdResult result = securityAnalysisService.checkQualityThresholds(projectId, sonarProjectKey);
            
            logger.info("成功完成质量阈值检查: projectId={}, 是否阻止合并={}", 
                projectId, result.isShouldBlockMerge());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("质量阈值检查失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 综合安全和性能分析
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 综合分析结果
     */
    @PostMapping("/analyze/comprehensive")
    public ResponseEntity<ComprehensiveAnalysisResult> comprehensiveAnalysis(
            @RequestParam String projectId,
            @RequestParam String sonarProjectKey) {
        
        try {
            logger.info("接收综合分析请求: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            ComprehensiveAnalysisResult result = new ComprehensiveAnalysisResult();
            result.setProjectId(projectId);
            result.setSonarProjectKey(sonarProjectKey);
            
            // 执行安全分析
            try {
                SecurityAnalysisResult securityResult = 
                    securityAnalysisService.analyzeSecurityVulnerabilities(projectId, sonarProjectKey);
                result.setSecurityAnalysis(securityResult);
            } catch (Exception e) {
                logger.error("综合分析中的安全分析失败: {}", e.getMessage(), e);
            }
            
            // 执行性能分析
            try {
                PerformanceAnalysisResult performanceResult = 
                    securityAnalysisService.analyzePerformanceIssues(projectId, sonarProjectKey);
                result.setPerformanceAnalysis(performanceResult);
            } catch (Exception e) {
                logger.error("综合分析中的性能分析失败: {}", e.getMessage(), e);
            }
            
            // 执行质量阈值检查
            try {
                QualityThresholdResult thresholdResult = 
                    securityAnalysisService.checkQualityThresholds(projectId, sonarProjectKey);
                result.setThresholdCheck(thresholdResult);
            } catch (Exception e) {
                logger.error("综合分析中的阈值检查失败: {}", e.getMessage(), e);
            }
            
            logger.info("成功完成综合分析: projectId={}", projectId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("综合分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 综合分析结果类
     */
    public static class ComprehensiveAnalysisResult {
        private String projectId;
        private String sonarProjectKey;
        private SecurityAnalysisResult securityAnalysis;
        private PerformanceAnalysisResult performanceAnalysis;
        private QualityThresholdResult thresholdCheck;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getSonarProjectKey() { return sonarProjectKey; }
        public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }
        
        public SecurityAnalysisResult getSecurityAnalysis() { return securityAnalysis; }
        public void setSecurityAnalysis(SecurityAnalysisResult securityAnalysis) { this.securityAnalysis = securityAnalysis; }
        
        public PerformanceAnalysisResult getPerformanceAnalysis() { return performanceAnalysis; }
        public void setPerformanceAnalysis(PerformanceAnalysisResult performanceAnalysis) { this.performanceAnalysis = performanceAnalysis; }
        
        public QualityThresholdResult getThresholdCheck() { return thresholdCheck; }
        public void setThresholdCheck(QualityThresholdResult thresholdCheck) { this.thresholdCheck = thresholdCheck; }
    }
}