package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.entity.CodeReview;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.repository.CodeReviewRepository;
import com.gitlab.metrics.repository.MergeRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 合并请求服务
 * 处理GitLab合并请求事件，实现评审流程管理
 */
@Service
@Transactional
public class MergeRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(MergeRequestService.class);
    
    @Autowired
    private MergeRequestRepository mergeRequestRepository;
    
    @Autowired
    private CodeReviewRepository codeReviewRepository;
    
    @Autowired
    private ReviewRuleEngine reviewRuleEngine;
    
    /**
     * 处理合并请求事件
     */
    public void processMergeRequestEvent(MergeRequestEventRequest event) {
        try {
            logger.info("Processing merge request event: MR {} - Action: {}", 
                       event.getObjectAttributes().getId(), 
                       event.getObjectAttributes().getAction());
            
            MergeRequest mergeRequest = processBasicMergeRequestInfo(event);
            
            // 根据不同的动作处理评审流程
            String action = event.getObjectAttributes().getAction();
            switch (action) {
                case "open":
                    handleMergeRequestOpened(mergeRequest, event);
                    break;
                case "update":
                    handleMergeRequestUpdated(mergeRequest, event);
                    break;
                case "merge":
                    handleMergeRequestMerged(mergeRequest, event);
                    break;
                case "close":
                    handleMergeRequestClosed(mergeRequest, event);
                    break;
                case "reopen":
                    handleMergeRequestReopened(mergeRequest, event);
                    break;
                default:
                    logger.debug("Unhandled merge request action: {}", action);
            }
            
            logger.info("Successfully processed merge request event for MR: {}", mergeRequest.getMrId());
            
        } catch (Exception e) {
            logger.error("Failed to process merge request event", e);
            throw new RuntimeException("Failed to process merge request event: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理基本的合并请求信息
     */
    private MergeRequest processBasicMergeRequestInfo(MergeRequestEventRequest event) {
        MergeRequestEventRequest.MergeRequestAttributes attrs = event.getObjectAttributes();
        String mrId = String.valueOf(attrs.getId());
        
        Optional<MergeRequest> existingMR = mergeRequestRepository.findByMrId(mrId);
        MergeRequest mergeRequest;
        
        if (existingMR.isPresent()) {
            mergeRequest = existingMR.get();
            logger.debug("Updating existing merge request: {}", mrId);
        } else {
            mergeRequest = new MergeRequest();
            mergeRequest.setMrId(mrId);
            logger.debug("Creating new merge request: {}", mrId);
        }
        
        // 更新基本信息
        mergeRequest.setProjectId(String.valueOf(attrs.getTargetProjectId()));
        mergeRequest.setAuthorId(String.valueOf(attrs.getAuthorId()));
        mergeRequest.setAuthorName(event.getUser() != null ? event.getUser().getName() : "Unknown");
        mergeRequest.setTitle(attrs.getTitle());
        mergeRequest.setDescription(attrs.getDescription());
        mergeRequest.setSourceBranch(attrs.getSourceBranch());
        mergeRequest.setTargetBranch(attrs.getTargetBranch());
        mergeRequest.setStatus(mapGitLabStateToStatus(attrs.getState()));
        mergeRequest.setUpdatedAt(LocalDateTime.now());
        
        // 设置时间戳
        if (attrs.getCreatedAt() != null) {
            mergeRequest.setCreatedAt(parseDateTime(attrs.getCreatedAt()));
        }
        
        // 设置合并信息
        if ("merged".equals(attrs.getState()) && attrs.getUpdatedAt() != null) {
            mergeRequest.setMergedAt(parseDateTime(attrs.getUpdatedAt()));
            if (event.getUser() != null) {
                mergeRequest.setMergedById(String.valueOf(event.getUser().getId()));
                mergeRequest.setMergedByName(event.getUser().getName());
            }
        }
        
        // 设置关闭信息
        if ("closed".equals(attrs.getState()) && attrs.getUpdatedAt() != null) {
            mergeRequest.setClosedAt(parseDateTime(attrs.getUpdatedAt()));
        }
        
        // 设置分配人信息
        if (attrs.getAssigneeId() != null) {
            mergeRequest.setAssigneeId(String.valueOf(attrs.getAssigneeId()));
            // 这里可以通过GitLab API获取分配人姓名，暂时留空
        }
        
        return mergeRequestRepository.save(mergeRequest);
    }
    
    /**
     * 处理合并请求打开事件
     */
    private void handleMergeRequestOpened(MergeRequest mergeRequest, MergeRequestEventRequest event) {
        logger.info("Merge request opened: {}", mergeRequest.getMrId());
        
        // 检查是否需要强制评审
        if (isReviewRequired(mergeRequest)) {
            logger.info("Review is required for merge request: {}", mergeRequest.getMrId());
            // 这里可以添加通知逻辑，提醒需要评审
        }
        
        // 记录合并请求创建的审计日志
        logMergeRequestActivity(mergeRequest, "opened", "Merge request created");
    }
    
    /**
     * 处理合并请求更新事件
     */
    private void handleMergeRequestUpdated(MergeRequest mergeRequest, MergeRequestEventRequest event) {
        logger.info("Merge request updated: {}", mergeRequest.getMrId());
        
        // 如果有代码变更，可能需要重新评审
        if (hasSignificantChanges(event)) {
            logger.info("Significant changes detected in merge request: {}", mergeRequest.getMrId());
            // 这里可以添加重新评审的逻辑
        }
        
        logMergeRequestActivity(mergeRequest, "updated", "Merge request updated");
    }
    
    /**
     * 处理合并请求合并事件
     */
    private void handleMergeRequestMerged(MergeRequest mergeRequest, MergeRequestEventRequest event) {
        logger.info("Merge request merged: {}", mergeRequest.getMrId());
        
        // 检查是否满足评审要求
        if (!isReviewApprovalSatisfied(mergeRequest)) {
            logger.warn("Merge request {} was merged without proper review approval", mergeRequest.getMrId());
            // 这里可以添加告警逻辑
        }
        
        // 计算评审时间统计
        calculateReviewMetrics(mergeRequest);
        
        logMergeRequestActivity(mergeRequest, "merged", "Merge request merged");
    }
    
    /**
     * 处理合并请求关闭事件
     */
    private void handleMergeRequestClosed(MergeRequest mergeRequest, MergeRequestEventRequest event) {
        logger.info("Merge request closed: {}", mergeRequest.getMrId());
        logMergeRequestActivity(mergeRequest, "closed", "Merge request closed");
    }
    
    /**
     * 处理合并请求重新打开事件
     */
    private void handleMergeRequestReopened(MergeRequest mergeRequest, MergeRequestEventRequest event) {
        logger.info("Merge request reopened: {}", mergeRequest.getMrId());
        logMergeRequestActivity(mergeRequest, "reopened", "Merge request reopened");
    }
    
    /**
     * 添加代码评审记录
     */
    public CodeReview addCodeReview(String mrId, String reviewerId, String reviewerName, 
                                   String status, String comment) {
        Optional<MergeRequest> mergeRequestOpt = mergeRequestRepository.findByMrId(mrId);
        if (!mergeRequestOpt.isPresent()) {
            throw new IllegalArgumentException("Merge request not found: " + mrId);
        }
        
        MergeRequest mergeRequest = mergeRequestOpt.get();
        
        CodeReview review = new CodeReview();
        review.setMergeRequest(mergeRequest);
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewedAt(LocalDateTime.now());
        review.setStatus(status);
        review.setComment(comment);
        review.setReviewType("manual");
        review.setIsRequired(isReviewRequired(mergeRequest));
        
        CodeReview savedReview = codeReviewRepository.save(review);
        
        logger.info("Added code review for MR {}: {} by {}", mrId, status, reviewerName);
        
        return savedReview;
    }
    
    /**
     * 检查合并请求是否需要评审
     */
    public boolean isReviewRequired(MergeRequest mergeRequest) {
        // 基本规则：所有合并到主分支的请求都需要评审
        if ("main".equals(mergeRequest.getTargetBranch()) || 
            "master".equals(mergeRequest.getTargetBranch()) ||
            "develop".equals(mergeRequest.getTargetBranch())) {
            return true;
        }
        
        // 可以根据项目配置添加更多规则
        return false;
    }
    
    /**
     * 检查评审批准是否满足要求
     */
    public boolean isReviewApprovalSatisfied(MergeRequest mergeRequest) {
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(mergeRequest.getId());
        
        if (reviews.isEmpty()) {
            return !isReviewRequired(mergeRequest);
        }
        
        // 检查是否有至少一个批准
        boolean hasApproval = reviews.stream()
            .anyMatch(review -> "approved".equals(review.getStatus()));
        
        // 检查是否有未解决的变更请求
        boolean hasUnresolvedChanges = reviews.stream()
            .anyMatch(review -> "changes_requested".equals(review.getStatus()));
        
        return hasApproval && !hasUnresolvedChanges;
    }
    
    /**
     * 获取合并请求的评审状态
     */
    public String getReviewStatus(String mrId) {
        Optional<MergeRequest> mergeRequestOpt = mergeRequestRepository.findByMrId(mrId);
        if (!mergeRequestOpt.isPresent()) {
            return "not_found";
        }
        
        MergeRequest mergeRequest = mergeRequestOpt.get();
        
        if (!isReviewRequired(mergeRequest)) {
            return "not_required";
        }
        
        if (isReviewApprovalSatisfied(mergeRequest)) {
            return "approved";
        }
        
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(mergeRequest.getId());
        
        if (reviews.isEmpty()) {
            return "pending";
        }
        
        boolean hasChangesRequested = reviews.stream()
            .anyMatch(review -> "changes_requested".equals(review.getStatus()));
        
        if (hasChangesRequested) {
            return "changes_requested";
        }
        
        return "in_review";
    }
    
    /**
     * 检查合并请求是否可以合并（使用评审规则引擎）
     */
    public ReviewRuleEngine.ReviewRuleResult checkCanMerge(String mrId) {
        return reviewRuleEngine.canMerge(mrId);
    }
    
    /**
     * 授权紧急绕过
     */
    public ReviewRuleEngine.EmergencyBypassResult authorizeEmergencyBypass(String mrId, String adminUserId, String reason) {
        return reviewRuleEngine.authorizeEmergencyBypass(mrId, adminUserId, reason);
    }
    
    /**
     * 计算评审覆盖率统计
     */
    public ReviewRuleEngine.ReviewCoverageStats calculateReviewCoverage(String projectId, LocalDateTime start, LocalDateTime end) {
        return reviewRuleEngine.calculateReviewCoverage(projectId, start, end);
    }
    
    /**
     * 强制执行评审规则检查（在合并前调用）
     */
    public boolean enforceReviewRules(String mrId) {
        try {
            ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge(mrId);
            
            if (!result.isCanMerge()) {
                logger.warn("Merge blocked for MR {} due to review rule violations: {}", 
                           mrId, result.getViolations());
                return false;
            }
            
            logger.info("Review rules satisfied for MR: {}", mrId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to enforce review rules for MR {}: {}", mrId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 计算评审指标
     */
    private void calculateReviewMetrics(MergeRequest mergeRequest) {
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(mergeRequest.getId());
        
        if (!reviews.isEmpty()) {
            // 计算首次评审响应时间
            CodeReview firstReview = reviews.get(reviews.size() - 1);
            long responseHours = java.time.Duration.between(mergeRequest.getCreatedAt(), firstReview.getReviewedAt()).toHours();
            
            logger.info("Review metrics for MR {}: First response time: {} hours, Total reviews: {}", 
                       mergeRequest.getMrId(), responseHours, reviews.size());
        }
    }
    
    /**
     * 检查是否有重要变更
     */
    private boolean hasSignificantChanges(MergeRequestEventRequest event) {
        // 这里可以根据变更的文件数量、行数等判断是否为重要变更
        // 暂时返回false，后续可以扩展
        return false;
    }
    
    /**
     * 记录合并请求活动日志
     */
    private void logMergeRequestActivity(MergeRequest mergeRequest, String action, String description) {
        logger.info("MR Activity - ID: {}, Project: {}, Action: {}, Description: {}", 
                   mergeRequest.getMrId(), mergeRequest.getProjectId(), action, description);
    }
    
    /**
     * 映射GitLab状态到内部状态
     */
    private String mapGitLabStateToStatus(String gitlabState) {
        switch (gitlabState) {
            case "opened":
                return "opened";
            case "merged":
                return "merged";
            case "closed":
                return "closed";
            default:
                return gitlabState;
        }
    }
    
    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // GitLab使用ISO 8601格式
            return LocalDateTime.parse(dateTimeStr.replace("Z", ""), 
                                     DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr.replace("Z", ""), 
                                         DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (Exception e2) {
                logger.warn("Failed to parse datetime: {}", dateTimeStr);
                return LocalDateTime.now();
            }
        }
    }
}