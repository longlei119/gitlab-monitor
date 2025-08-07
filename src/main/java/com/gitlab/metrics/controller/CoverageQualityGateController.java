package com.gitlab.metrics.controller;

import com.gitlab.metrics.service.CoverageQualityGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 覆盖率质量门禁控制器
 * 提供覆盖率质量门禁相关的REST API接口
 */
@RestController
@RequestMapping("/api/coverage/quality-gate")
public class CoverageQualityGateController {
    
    private static final Logger logger = LoggerFactory.getLogger(CoverageQualityGateController.class);
    
    @Autowired
    private CoverageQualityGateService qualityGateService;
    
    /**
     * 检查覆盖率质量门禁
     */
    @GetMapping("/check")
    public ResponseEntity<CoverageQualityGateService.QualityGateResult> checkQualityGate(
            @RequestParam String projectId,
            @RequestParam String commitSha) {
        
        logger.info("收到覆盖率质量门禁检查请求: projectId={}, commitSha={}", projectId, commitSha);
        
        try {
            CoverageQualityGateService.QualityGateResult result = 
                qualityGateService.checkQualityGate(projectId, commitSha);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("覆盖率质量门禁检查失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查覆盖率质量门禁（自定义阈值）
     */
    @GetMapping("/check/custom")
    public ResponseEntity<CoverageQualityGateService.QualityGateResult> checkQualityGateWithCustomThreshold(
            @RequestParam String projectId,
            @RequestParam String commitSha,
            @RequestParam(required = false) Double lineThreshold,
            @RequestParam(required = false) Double branchThreshold,
            @RequestParam(required = false) Double functionThreshold) {
        
        logger.info("收到自定义阈值覆盖率质量门禁检查请求: projectId={}, commitSha={}, lineThreshold={}, branchThreshold={}, functionThreshold={}", 
                   projectId, commitSha, lineThreshold, branchThreshold, functionThreshold);
        
        try {
            CoverageQualityGateService.QualityGateResult result = 
                qualityGateService.checkQualityGate(projectId, commitSha, lineThreshold, branchThreshold, functionThreshold);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("自定义阈值覆盖率质量门禁检查失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查新增代码测试要求
     */
    @GetMapping("/check/new-code")
    public ResponseEntity<CoverageQualityGateService.NewCodeTestResult> checkNewCodeTestRequirement(
            @RequestParam String projectId,
            @RequestParam String commitSha,
            @RequestParam Integer newCodeLines) {
        
        logger.info("收到新增代码测试检查请求: projectId={}, commitSha={}, newCodeLines={}", 
                   projectId, commitSha, newCodeLines);
        
        try {
            CoverageQualityGateService.NewCodeTestResult result = 
                qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, newCodeLines);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("新增代码测试检查失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查测试失败情况
     */
    @PostMapping("/check/test-failures")
    public ResponseEntity<CoverageQualityGateService.TestFailureResult> checkTestFailures(
            @RequestParam String projectId,
            @RequestParam String commitSha,
            @RequestBody CoverageQualityGateService.TestResults testResults) {
        
        logger.info("收到测试失败检查请求: projectId={}, commitSha={}, totalTests={}, failedTests={}", 
                   projectId, commitSha, testResults.getTotalTests(), testResults.getFailedTests());
        
        try {
            CoverageQualityGateService.TestFailureResult result = 
                qualityGateService.checkTestFailures(projectId, commitSha, testResults);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("测试失败检查失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 阻止部署
     */
    @PostMapping("/block-deployment")
    public ResponseEntity<String> blockDeployment(
            @RequestParam String projectId,
            @RequestParam String commitSha,
            @RequestParam String reason) {
        
        logger.info("收到阻止部署请求: projectId={}, commitSha={}, reason={}", projectId, commitSha, reason);
        
        try {
            qualityGateService.blockDeployment(projectId, commitSha, reason);
            return ResponseEntity.ok("部署已被阻止");
            
        } catch (Exception e) {
            logger.error("阻止部署失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("阻止部署失败");
        }
    }
    
    /**
     * 获取质量门禁统计
     */
    @GetMapping("/stats")
    public ResponseEntity<CoverageQualityGateService.QualityGateStats> getQualityGateStats(
            @RequestParam String projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("收到质量门禁统计请求: projectId={}, startDate={}, endDate={}", 
                   projectId, startDate, endDate);
        
        try {
            CoverageQualityGateService.QualityGateStats stats = 
                qualityGateService.getQualityGateStats(projectId, startDate, endDate);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取质量门禁统计失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}