package com.gitlab.metrics.controller;

import com.gitlab.metrics.service.MergeRequestService;
import com.gitlab.metrics.service.ReviewRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 评审规则控制器
 * 提供评审规则检查和管理的REST API接口
 */
@RestController
@RequestMapping("/api/review-rules")
public class ReviewRuleController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewRuleController.class);
    
    @Autowired
    private MergeRequestService mergeRequestService;
    
    @Autowired
    private ReviewRuleEngine reviewRuleEngine;
    
    /**
     * 检查合并请求是否可以合并
     * 
     * @param mrId 合并请求ID
     * @return 评审规则检查结果
     */
    @GetMapping("/check-merge/{mrId}")
    public ResponseEntity<ReviewRuleEngine.ReviewRuleResult> checkCanMerge(@PathVariable String mrId) {
        try {
            logger.info("检查合并规则: mrId={}", mrId);
            
            ReviewRuleEngine.ReviewRuleResult result = mergeRequestService.checkCanMerge(mrId);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("合并请求不存在: mrId={}", mrId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("检查合并规则失败: mrId={}, error={}", mrId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 强制执行评审规则检查
     * 
     * @param mrId 合并请求ID
     * @return 是否可以合并
     */
    @PostMapping("/enforce/{mrId}")
    public ResponseEntity<EnforceResult> enforceReviewRules(@PathVariable String mrId) {
        try {
            logger.info("强制执行评审规则: mrId={}", mrId);
            
            boolean canMerge = mergeRequestService.enforceReviewRules(mrId);
            
            EnforceResult result = new EnforceResult();
            result.setMrId(mrId);
            result.setCanMerge(canMerge);
            result.setMessage(canMerge ? "Review rules satisfied" : "Review rules violated");
            result.setCheckedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("合并请求不存在: mrId={}", mrId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("强制执行评审规则失败: mrId={}, error={}", mrId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 授权紧急绕过
     * 
     * @param mrId 合并请求ID
     * @param request 紧急绕过请求
     * @return 紧急绕过结果
     */
    @PostMapping("/emergency-bypass/{mrId}")
    public ResponseEntity<ReviewRuleEngine.EmergencyBypassResult> authorizeEmergencyBypass(
            @PathVariable String mrId,
            @RequestBody EmergencyBypassRequest request) {
        
        try {
            logger.info("授权紧急绕过: mrId={}, adminUser={}", mrId, request.getAdminUserId());
            
            ReviewRuleEngine.EmergencyBypassResult result = mergeRequestService.authorizeEmergencyBypass(
                mrId, request.getAdminUserId(), request.getReason());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("紧急绕过授权失败: mrId={}, error={}", mrId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("紧急绕过未启用: mrId={}", mrId);
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("授权紧急绕过失败: mrId={}, error={}", mrId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取评审覆盖率统计
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 评审覆盖率统计
     */
    @GetMapping("/coverage-stats")
    public ResponseEntity<ReviewRuleEngine.ReviewCoverageStats> getReviewCoverageStats(
            @RequestParam String projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            logger.info("获取评审覆盖率统计: projectId={}, period=[{} - {}]", 
                       projectId, startDate, endDate);
            
            ReviewRuleEngine.ReviewCoverageStats stats = mergeRequestService.calculateReviewCoverage(
                projectId, startDate, endDate);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取评审覆盖率统计失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取合并请求的评审状态
     * 
     * @param mrId 合并请求ID
     * @return 评审状态
     */
    @GetMapping("/status/{mrId}")
    public ResponseEntity<ReviewStatusResult> getReviewStatus(@PathVariable String mrId) {
        try {
            logger.info("获取评审状态: mrId={}", mrId);
            
            String status = mergeRequestService.getReviewStatus(mrId);
            
            ReviewStatusResult result = new ReviewStatusResult();
            result.setMrId(mrId);
            result.setStatus(status);
            result.setCheckedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取评审状态失败: mrId={}, error={}", mrId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // DTO类定义
    
    /**
     * 紧急绕过请求
     */
    public static class EmergencyBypassRequest {
        private String adminUserId;
        private String reason;
        
        // Getters and Setters
        public String getAdminUserId() { return adminUserId; }
        public void setAdminUserId(String adminUserId) { this.adminUserId = adminUserId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    /**
     * 强制执行结果
     */
    public static class EnforceResult {
        private String mrId;
        private boolean canMerge;
        private String message;
        private LocalDateTime checkedAt;
        
        // Getters and Setters
        public String getMrId() { return mrId; }
        public void setMrId(String mrId) { this.mrId = mrId; }
        
        public boolean isCanMerge() { return canMerge; }
        public void setCanMerge(boolean canMerge) { this.canMerge = canMerge; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getCheckedAt() { return checkedAt; }
        public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    }
    
    /**
     * 评审状态结果
     */
    public static class ReviewStatusResult {
        private String mrId;
        private String status;
        private LocalDateTime checkedAt;
        
        // Getters and Setters
        public String getMrId() { return mrId; }
        public void setMrId(String mrId) { this.mrId = mrId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getCheckedAt() { return checkedAt; }
        public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    }
}