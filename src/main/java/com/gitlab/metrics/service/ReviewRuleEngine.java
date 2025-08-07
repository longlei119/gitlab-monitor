package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.CodeReview;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.repository.CodeReviewRepository;
import com.gitlab.metrics.repository.MergeRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 评审规则引擎
 * 负责强制执行代码评审制度和规则
 */
@Service
public class ReviewRuleEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewRuleEngine.class);
    
    @Autowired
    private MergeRequestRepository mergeRequestRepository;
    
    @Autowired
    private CodeReviewRepository codeReviewRepository;
    
    @Autowired
    private AlertService alertService;
    
    // 配置参数
    @Value("${review.rules.protected-branches:main,master,develop,release}")
    private String protectedBranches;
    
    @Value("${review.rules.min-reviewers:1}")
    private Integer minReviewers;
    
    @Value("${review.rules.require-approval:true}")
    private Boolean requireApproval;
    
    @Value("${review.rules.block-self-approval:true}")
    private Boolean blockSelfApproval;
    
    @Value("${review.rules.admin-users:}")
    private String adminUsers;
    
    @Value("${review.rules.emergency-bypass-enabled:false}")
    private Boolean emergencyBypassEnabled;
    
    @Value("${review.rules.large-mr-threshold:500}")
    private Integer largeMrThreshold;
    
    @Value("${review.rules.large-mr-min-reviewers:2}")
    private Integer largeMrMinReviewers;
    
    /**
     * 检查合并请求是否可以合并
     */
    public ReviewRuleResult canMerge(String mrId) {
        try {
            logger.info("Checking merge rules for MR: {}", mrId);
            
            MergeRequest mergeRequest = mergeRequestRepository.findByMrId(mrId)
                .orElseThrow(() -> new IllegalArgumentException("Merge request not found: " + mrId));
            
            ReviewRuleResult result = new ReviewRuleResult();
            result.setMrId(mrId);
            result.setCanMerge(true);
            result.setCheckedAt(LocalDateTime.now());
            
            // 检查是否需要评审
            if (!isReviewRequired(mergeRequest)) {
                result.addMessage("Review not required for this branch");
                return result;
            }
            
            // 检查紧急绕过
            if (hasEmergencyBypass(mergeRequest)) {
                result.addMessage("Emergency bypass authorized");
                result.setEmergencyBypass(true);
                return result;
            }
            
            // 获取评审记录
            List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(mergeRequest.getId());
            
            // 检查最小评审者数量
            if (!hasMinimumReviewers(mergeRequest, reviews)) {
                result.setCanMerge(false);
                result.addViolation("Insufficient reviewers", 
                    String.format("Requires at least %d reviewers", getRequiredReviewers(mergeRequest)));
            }
            
            // 检查是否有批准
            if (!hasRequiredApprovals(mergeRequest, reviews)) {
                result.setCanMerge(false);
                result.addViolation("Missing required approvals", 
                    "At least one approval is required");
            }
            
            // 检查是否有未解决的变更请求
            if (hasUnresolvedChangeRequests(reviews)) {
                result.setCanMerge(false);
                result.addViolation("Unresolved change requests", 
                    "All change requests must be resolved");
            }
            
            // 检查自我批准
            if (hasSelfApproval(mergeRequest, reviews)) {
                result.setCanMerge(false);
                result.addViolation("Self-approval not allowed", 
                    "Author cannot approve their own merge request");
            }
            
            // 检查评审者资格
            if (!hasQualifiedReviewers(mergeRequest, reviews)) {
                result.setCanMerge(false);
                result.addViolation("Unqualified reviewers", 
                    "Reviewers must have appropriate permissions");
            }
            
            // 记录检查结果
            if (!result.isCanMerge()) {
                logger.warn("Merge blocked for MR {}: {}", mrId, result.getViolations());
                sendMergeBlockedAlert(mergeRequest, result);
            } else {
                logger.info("Merge approved for MR: {}", mrId);
            }
            
            return result;
            
        } catch (IllegalArgumentException e) {
            logger.error("Failed to check merge rules for MR {}: {}", mrId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to check merge rules for MR {}: {}", mrId, e.getMessage(), e);
            throw new RuntimeException("Failed to check merge rules: " + e.getMessage(), e);
        }
    }
    
    /**
     * 授权紧急绕过
     */
    public EmergencyBypassResult authorizeEmergencyBypass(String mrId, String adminUserId, String reason) {
        try {
            logger.info("Authorizing emergency bypass for MR {} by admin {}", mrId, adminUserId);
            
            if (!emergencyBypassEnabled) {
                throw new IllegalStateException("Emergency bypass is not enabled");
            }
            
            if (!isAdminUser(adminUserId)) {
                throw new IllegalArgumentException("User is not authorized for emergency bypass: " + adminUserId);
            }
            
            MergeRequest mergeRequest = mergeRequestRepository.findByMrId(mrId)
                .orElseThrow(() -> new IllegalArgumentException("Merge request not found: " + mrId));
            
            // 创建紧急绕过记录
            EmergencyBypass bypass = new EmergencyBypass();
            bypass.setMrId(mrId);
            bypass.setProjectId(mergeRequest.getProjectId());
            bypass.setAuthorizedBy(adminUserId);
            bypass.setReason(reason);
            bypass.setAuthorizedAt(LocalDateTime.now());
            bypass.setActive(true);
            
            // 这里可以保存到数据库，暂时记录日志
            logger.info("Emergency bypass authorized: MR={}, Admin={}, Reason={}", 
                       mrId, adminUserId, reason);
            
            // 发送告警通知
            sendEmergencyBypassAlert(mergeRequest, bypass);
            
            EmergencyBypassResult result = new EmergencyBypassResult();
            result.setMrId(mrId);
            result.setAuthorized(true);
            result.setBypass(bypass);
            result.setMessage("Emergency bypass authorized successfully");
            
            return result;
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.error("Failed to authorize emergency bypass for MR {}: {}", mrId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to authorize emergency bypass for MR {}: {}", mrId, e.getMessage(), e);
            throw new RuntimeException("Failed to authorize emergency bypass: " + e.getMessage(), e);
        }
    }
    
    /**
     * 计算评审覆盖率统计
     */
    public ReviewCoverageStats calculateReviewCoverage(String projectId, LocalDateTime start, LocalDateTime end) {
        try {
            logger.info("Calculating review coverage for project {} from {} to {}", projectId, start, end);
            
            // 获取时间范围内的所有合并请求
            List<MergeRequest> mergeRequests = mergeRequestRepository.findByProjectIdAndCreatedAtBetween(
                projectId, start, end);
            
            ReviewCoverageStats stats = new ReviewCoverageStats();
            stats.setProjectId(projectId);
            stats.setPeriodStart(start);
            stats.setPeriodEnd(end);
            stats.setCalculatedAt(LocalDateTime.now());
            
            int totalMRs = mergeRequests.size();
            int reviewedMRs = 0;
            int approvedMRs = 0;
            int rejectedMRs = 0;
            long totalReviewTime = 0;
            int totalReviews = 0;
            
            for (MergeRequest mr : mergeRequests) {
                List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(mr.getId());
                
                if (!reviews.isEmpty()) {
                    reviewedMRs++;
                    totalReviews += reviews.size();
                    
                    // 计算评审时间
                    CodeReview firstReview = reviews.get(reviews.size() - 1);
                    long reviewTimeHours = java.time.Duration.between(mr.getCreatedAt(), firstReview.getReviewedAt()).toHours();
                    totalReviewTime += reviewTimeHours;
                    
                    // 统计批准和拒绝
                    boolean hasApproval = reviews.stream().anyMatch(r -> "approved".equals(r.getStatus()));
                    boolean hasRejection = reviews.stream().anyMatch(r -> "changes_requested".equals(r.getStatus()));
                    
                    if (hasApproval) approvedMRs++;
                    if (hasRejection) rejectedMRs++;
                }
            }
            
            // 计算统计指标
            stats.setTotalMergeRequests(totalMRs);
            stats.setReviewedMergeRequests(reviewedMRs);
            stats.setApprovedMergeRequests(approvedMRs);
            stats.setRejectedMergeRequests(rejectedMRs);
            stats.setTotalReviews(totalReviews);
            
            if (totalMRs > 0) {
                stats.setReviewCoverageRate((double) reviewedMRs / totalMRs * 100);
                stats.setApprovalRate((double) approvedMRs / totalMRs * 100);
                stats.setRejectionRate((double) rejectedMRs / totalMRs * 100);
            }
            
            if (reviewedMRs > 0) {
                stats.setAverageReviewTimeHours((double) totalReviewTime / reviewedMRs);
                stats.setAverageReviewsPerMR((double) totalReviews / reviewedMRs);
            }
            
            logger.info("Review coverage calculated: {}% coverage, {} average review time hours", 
                       stats.getReviewCoverageRate(), stats.getAverageReviewTimeHours());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to calculate review coverage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate review coverage: " + e.getMessage(), e);
        }
    }
    
    // 私有辅助方法
    
    private boolean isReviewRequired(MergeRequest mergeRequest) {
        Set<String> protectedBranchSet = new HashSet<>(Arrays.asList(protectedBranches.split(",")));
        return protectedBranchSet.contains(mergeRequest.getTargetBranch());
    }
    
    private boolean hasEmergencyBypass(MergeRequest mergeRequest) {
        // 这里可以检查数据库中的紧急绕过记录
        // 暂时返回false
        return false;
    }
    
    private boolean hasMinimumReviewers(MergeRequest mergeRequest, List<CodeReview> reviews) {
        int requiredReviewers = getRequiredReviewers(mergeRequest);
        long uniqueReviewers = reviews.stream()
            .map(CodeReview::getReviewerId)
            .distinct()
            .count();
        return uniqueReviewers >= requiredReviewers;
    }
    
    private int getRequiredReviewers(MergeRequest mergeRequest) {
        // 检查是否为大型MR
        int totalChanges = (mergeRequest.getAdditions() != null ? mergeRequest.getAdditions() : 0) +
                          (mergeRequest.getDeletions() != null ? mergeRequest.getDeletions() : 0);
        
        if (totalChanges > largeMrThreshold) {
            return largeMrMinReviewers;
        }
        
        return minReviewers;
    }
    
    private boolean hasRequiredApprovals(MergeRequest mergeRequest, List<CodeReview> reviews) {
        if (!requireApproval) {
            return true;
        }
        
        return reviews.stream().anyMatch(review -> "approved".equals(review.getStatus()));
    }
    
    private boolean hasUnresolvedChangeRequests(List<CodeReview> reviews) {
        return reviews.stream().anyMatch(review -> "changes_requested".equals(review.getStatus()));
    }
    
    private boolean hasSelfApproval(MergeRequest mergeRequest, List<CodeReview> reviews) {
        if (!blockSelfApproval) {
            return false;
        }
        
        return reviews.stream()
            .anyMatch(review -> "approved".equals(review.getStatus()) && 
                               mergeRequest.getAuthorId().equals(review.getReviewerId()));
    }
    
    private boolean hasQualifiedReviewers(MergeRequest mergeRequest, List<CodeReview> reviews) {
        // 这里可以添加评审者资格检查逻辑
        // 暂时返回true
        return true;
    }
    
    private boolean isAdminUser(String userId) {
        if (adminUsers == null || adminUsers.trim().isEmpty()) {
            return false;
        }
        
        Set<String> adminUserSet = new HashSet<>(Arrays.asList(adminUsers.split(",")));
        return adminUserSet.contains(userId);
    }
    
    private void sendMergeBlockedAlert(MergeRequest mergeRequest, ReviewRuleResult result) {
        try {
            AlertService.Alert alert = new AlertService.Alert();
            alert.setType(AlertService.AlertType.MERGE_BLOCKED);
            alert.setLevel(AlertService.AlertLevel.HIGH);
            alert.setProjectId(mergeRequest.getProjectId());
            alert.setRelatedEntityId(mergeRequest.getMrId());
            alert.setTitle("合并被阻止");
            alert.setMessage(String.format("合并请求 %s 由于评审规则违规被阻止合并", mergeRequest.getMrId()));
            alert.setCreatedAt(LocalDateTime.now());
            alert.setDetails(result);
            
            alertService.sendAlert(alert);
            
        } catch (Exception e) {
            logger.error("Failed to send merge blocked alert: {}", e.getMessage(), e);
        }
    }
    
    private void sendEmergencyBypassAlert(MergeRequest mergeRequest, EmergencyBypass bypass) {
        try {
            AlertService.Alert alert = new AlertService.Alert();
            alert.setType(AlertService.AlertType.MERGE_BLOCKED); // 可以添加新的类型
            alert.setLevel(AlertService.AlertLevel.CRITICAL);
            alert.setProjectId(mergeRequest.getProjectId());
            alert.setRelatedEntityId(mergeRequest.getMrId());
            alert.setTitle("紧急绕过授权");
            alert.setMessage(String.format("合并请求 %s 获得紧急绕过授权，授权人: %s", 
                           mergeRequest.getMrId(), bypass.getAuthorizedBy()));
            alert.setCreatedAt(LocalDateTime.now());
            alert.setDetails(bypass);
            
            alertService.sendAlert(alert);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency bypass alert: {}", e.getMessage(), e);
        }
    }
    
    // 内部类定义
    
    /**
     * 评审规则检查结果
     */
    public static class ReviewRuleResult {
        private String mrId;
        private boolean canMerge;
        private boolean emergencyBypass;
        private LocalDateTime checkedAt;
        private List<String> messages = new java.util.ArrayList<>();
        private List<RuleViolation> violations = new java.util.ArrayList<>();
        
        public void addMessage(String message) {
            this.messages.add(message);
        }
        
        public void addViolation(String rule, String description) {
            this.violations.add(new RuleViolation(rule, description));
        }
        
        // Getters and Setters
        public String getMrId() { return mrId; }
        public void setMrId(String mrId) { this.mrId = mrId; }
        
        public boolean isCanMerge() { return canMerge; }
        public void setCanMerge(boolean canMerge) { this.canMerge = canMerge; }
        
        public boolean isEmergencyBypass() { return emergencyBypass; }
        public void setEmergencyBypass(boolean emergencyBypass) { this.emergencyBypass = emergencyBypass; }
        
        public LocalDateTime getCheckedAt() { return checkedAt; }
        public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
        
        public List<String> getMessages() { return messages; }
        public void setMessages(List<String> messages) { this.messages = messages; }
        
        public List<RuleViolation> getViolations() { return violations; }
        public void setViolations(List<RuleViolation> violations) { this.violations = violations; }
    }
    
    /**
     * 规则违规记录
     */
    public static class RuleViolation {
        private String rule;
        private String description;
        
        public RuleViolation(String rule, String description) {
            this.rule = rule;
            this.description = description;
        }
        
        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        @Override
        public String toString() {
            return rule + ": " + description;
        }
    }
    
    /**
     * 紧急绕过记录
     */
    public static class EmergencyBypass {
        private String mrId;
        private String projectId;
        private String authorizedBy;
        private String reason;
        private LocalDateTime authorizedAt;
        private boolean active;
        
        // Getters and Setters
        public String getMrId() { return mrId; }
        public void setMrId(String mrId) { this.mrId = mrId; }
        
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public LocalDateTime getAuthorizedAt() { return authorizedAt; }
        public void setAuthorizedAt(LocalDateTime authorizedAt) { this.authorizedAt = authorizedAt; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
    
    /**
     * 紧急绕过结果
     */
    public static class EmergencyBypassResult {
        private String mrId;
        private boolean authorized;
        private EmergencyBypass bypass;
        private String message;
        
        // Getters and Setters
        public String getMrId() { return mrId; }
        public void setMrId(String mrId) { this.mrId = mrId; }
        
        public boolean isAuthorized() { return authorized; }
        public void setAuthorized(boolean authorized) { this.authorized = authorized; }
        
        public EmergencyBypass getBypass() { return bypass; }
        public void setBypass(EmergencyBypass bypass) { this.bypass = bypass; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 评审覆盖率统计
     */
    public static class ReviewCoverageStats {
        private String projectId;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private LocalDateTime calculatedAt;
        private Integer totalMergeRequests;
        private Integer reviewedMergeRequests;
        private Integer approvedMergeRequests;
        private Integer rejectedMergeRequests;
        private Integer totalReviews;
        private Double reviewCoverageRate;
        private Double approvalRate;
        private Double rejectionRate;
        private Double averageReviewTimeHours;
        private Double averageReviewsPerMR;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
        
        public LocalDateTime getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
        
        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
        
        public Integer getTotalMergeRequests() { return totalMergeRequests; }
        public void setTotalMergeRequests(Integer totalMergeRequests) { this.totalMergeRequests = totalMergeRequests; }
        
        public Integer getReviewedMergeRequests() { return reviewedMergeRequests; }
        public void setReviewedMergeRequests(Integer reviewedMergeRequests) { this.reviewedMergeRequests = reviewedMergeRequests; }
        
        public Integer getApprovedMergeRequests() { return approvedMergeRequests; }
        public void setApprovedMergeRequests(Integer approvedMergeRequests) { this.approvedMergeRequests = approvedMergeRequests; }
        
        public Integer getRejectedMergeRequests() { return rejectedMergeRequests; }
        public void setRejectedMergeRequests(Integer rejectedMergeRequests) { this.rejectedMergeRequests = rejectedMergeRequests; }
        
        public Integer getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }
        
        public Double getReviewCoverageRate() { return reviewCoverageRate; }
        public void setReviewCoverageRate(Double reviewCoverageRate) { this.reviewCoverageRate = reviewCoverageRate; }
        
        public Double getApprovalRate() { return approvalRate; }
        public void setApprovalRate(Double approvalRate) { this.approvalRate = approvalRate; }
        
        public Double getRejectionRate() { return rejectionRate; }
        public void setRejectionRate(Double rejectionRate) { this.rejectionRate = rejectionRate; }
        
        public Double getAverageReviewTimeHours() { return averageReviewTimeHours; }
        public void setAverageReviewTimeHours(Double averageReviewTimeHours) { this.averageReviewTimeHours = averageReviewTimeHours; }
        
        public Double getAverageReviewsPerMR() { return averageReviewsPerMR; }
        public void setAverageReviewsPerMR(Double averageReviewsPerMR) { this.averageReviewsPerMR = averageReviewsPerMR; }
    }
}