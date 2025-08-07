package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Issue分析服务
 * 处理GitLab Issue事件，跟踪Bug修复效率和问题处理时间线
 */
@Service
@Transactional
public class IssueAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(IssueAnalysisService.class);
    
    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private BugFixEfficiencyService bugFixEfficiencyService;
    
    /**
     * 处理Issue事件
     * 
     * @param issueEvent GitLab Issue事件数据
     * @return 处理结果
     */
    public IssueProcessResult processIssueEvent(IssueEventRequest issueEvent) {
        try {
            logger.info("处理Issue事件: projectId={}, issueId={}, action={}", 
                issueEvent.getProject().getId(), 
                issueEvent.getObjectAttributes().getId(),
                issueEvent.getObjectAttributes().getAction());
            
            IssueProcessResult result = new IssueProcessResult();
            result.setProjectId(String.valueOf(issueEvent.getProject().getId()));
            result.setIssueId(String.valueOf(issueEvent.getObjectAttributes().getId()));
            result.setAction(issueEvent.getObjectAttributes().getAction());
            result.setProcessTime(LocalDateTime.now());
            
            String action = issueEvent.getObjectAttributes().getAction();
            
            switch (action) {
                case "open":
                    result = handleIssueCreated(issueEvent, result);
                    break;
                case "update":
                    result = handleIssueUpdated(issueEvent, result);
                    break;
                case "close":
                    result = handleIssueClosed(issueEvent, result);
                    break;
                case "reopen":
                    result = handleIssueReopened(issueEvent, result);
                    break;
                default:
                    logger.debug("未处理的Issue事件类型: {}", action);
                    result.setSuccess(false);
                    result.setMessage("未支持的事件类型: " + action);
                    break;
            }
            
            logger.info("Issue事件处理完成: issueId={}, success={}", 
                result.getIssueId(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理Issue事件失败: {}", e.getMessage(), e);
            IssueProcessResult errorResult = new IssueProcessResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("处理失败: " + e.getMessage());
            return errorResult;
        }
    }    

    /**
     * 处理Issue创建事件
     */
    private IssueProcessResult handleIssueCreated(IssueEventRequest issueEvent, IssueProcessResult result) {
        try {
            IssueEventRequest.IssueAttributes attrs = issueEvent.getObjectAttributes();
            
            // 检查Issue是否已存在
            String issueId = String.valueOf(attrs.getId());
            Optional<Issue> existingIssue = issueRepository.findByIssueId(issueId);
            
            if (existingIssue.isPresent()) {
                logger.debug("Issue已存在，跳过创建: {}", issueId);
                result.setSuccess(true);
                result.setMessage("Issue已存在");
                result.setIssue(existingIssue.get());
                return result;
            }
            
            // 创建新Issue
            Issue issue = new Issue();
            issue.setIssueId(issueId);
            issue.setProjectId(String.valueOf(attrs.getProjectId()));
            issue.setTitle(attrs.getTitle());
            issue.setDescription(attrs.getDescription());
            issue.setStatus(attrs.getState());
            issue.setCreatedAt(parseDateTime(attrs.getCreatedAt()));
            issue.setUpdatedAt(parseDateTime(attrs.getUpdatedAt()));
            issue.setWebUrl(attrs.getUrl());
            
            // 设置作者信息
            if (issueEvent.getUser() != null) {
                issue.setAuthorId(String.valueOf(issueEvent.getUser().getId()));
                issue.setAuthorName(issueEvent.getUser().getName());
            }
            
            // 设置分配人信息
            if (attrs.getAssigneeId() != null && issueEvent.getAssignee() != null) {
                issue.setAssigneeId(String.valueOf(attrs.getAssigneeId()));
                issue.setAssigneeName(issueEvent.getAssignee().getName());
            }
            
            // 分析Issue类型和严重程度
            analyzeIssueTypeAndSeverity(issue, attrs);
            
            // 处理标签
            if (attrs.getLabels() != null && attrs.getLabels().length > 0) {
                issue.setLabels(String.join(",", attrs.getLabels()));
            }
            
            // 保存Issue
            Issue savedIssue = issueRepository.save(issue);
            
            result.setSuccess(true);
            result.setMessage("Issue创建成功");
            result.setIssue(savedIssue);
            
            logger.info("成功创建Issue: issueId={}, title={}", issueId, issue.getTitle());
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理Issue创建事件失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("创建失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 处理Issue更新事件
     */
    private IssueProcessResult handleIssueUpdated(IssueEventRequest issueEvent, IssueProcessResult result) {
        try {
            IssueEventRequest.IssueAttributes attrs = issueEvent.getObjectAttributes();
            String issueId = String.valueOf(attrs.getId());
            
            Optional<Issue> existingIssue = issueRepository.findByIssueId(issueId);
            
            if (!existingIssue.isPresent()) {
                // 如果Issue不存在，创建它
                logger.info("Issue不存在，创建新Issue: {}", issueId);
                return handleIssueCreated(issueEvent, result);
            }
            
            Issue issue = existingIssue.get();
            boolean hasChanges = false;
            
            // 更新基本信息
            if (!issue.getTitle().equals(attrs.getTitle())) {
                issue.setTitle(attrs.getTitle());
                hasChanges = true;
            }
            
            if (!issue.getStatus().equals(attrs.getState())) {
                String oldStatus = issue.getStatus();
                issue.setStatus(attrs.getState());
                hasChanges = true;
                
                // 记录状态变更时间
                if ("closed".equals(attrs.getState()) && !"closed".equals(oldStatus)) {
                    issue.setClosedAt(parseDateTime(attrs.getUpdatedAt()));
                    calculateResolutionTime(issue);
                }
            }
            
            // 更新分配人
            if (attrs.getAssigneeId() != null && issueEvent.getAssignee() != null) {
                String newAssigneeId = String.valueOf(attrs.getAssigneeId());
                if (!newAssigneeId.equals(issue.getAssigneeId())) {
                    issue.setAssigneeId(newAssigneeId);
                    issue.setAssigneeName(issueEvent.getAssignee().getName());
                    hasChanges = true;
                    
                    // 如果是首次分配，记录响应时间
                    if (issue.getFirstResponseAt() == null) {
                        issue.setFirstResponseAt(parseDateTime(attrs.getUpdatedAt()));
                        calculateResponseTime(issue);
                    }
                }
            }
            
            // 更新时间戳
            issue.setUpdatedAt(parseDateTime(attrs.getUpdatedAt()));
            
            if (hasChanges) {
                Issue savedIssue = issueRepository.save(issue);
                result.setIssue(savedIssue);
                logger.info("成功更新Issue: issueId={}", issueId);
            }
            
            result.setSuccess(true);
            result.setMessage("Issue更新成功");
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理Issue更新事件失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("更新失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 处理Issue关闭事件
     */
    private IssueProcessResult handleIssueClosed(IssueEventRequest issueEvent, IssueProcessResult result) {
        try {
            IssueEventRequest.IssueAttributes attrs = issueEvent.getObjectAttributes();
            String issueId = String.valueOf(attrs.getId());
            
            Optional<Issue> existingIssue = issueRepository.findByIssueId(issueId);
            
            if (!existingIssue.isPresent()) {
                logger.warn("尝试关闭不存在的Issue: {}", issueId);
                result.setSuccess(false);
                result.setMessage("Issue不存在");
                return result;
            }
            
            Issue issue = existingIssue.get();
            issue.setStatus("closed");
            issue.setClosedAt(parseDateTime(attrs.getClosedAt() != null ? attrs.getClosedAt() : attrs.getUpdatedAt()));
            issue.setUpdatedAt(parseDateTime(attrs.getUpdatedAt()));
            
            // 设置解决时间
            issue.setResolutionAt(issue.getClosedAt());
            calculateResolutionTime(issue);
            
            Issue savedIssue = issueRepository.save(issue);
            
            result.setSuccess(true);
            result.setMessage("Issue关闭成功");
            result.setIssue(savedIssue);
            
            logger.info("成功关闭Issue: issueId={}, 解决时间={}分钟", 
                issueId, issue.getResolutionTimeMinutes());
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理Issue关闭事件失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("关闭失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 处理Issue重新打开事件
     */
    private IssueProcessResult handleIssueReopened(IssueEventRequest issueEvent, IssueProcessResult result) {
        try {
            IssueEventRequest.IssueAttributes attrs = issueEvent.getObjectAttributes();
            String issueId = String.valueOf(attrs.getId());
            
            Optional<Issue> existingIssue = issueRepository.findByIssueId(issueId);
            
            if (!existingIssue.isPresent()) {
                logger.warn("尝试重新打开不存在的Issue: {}", issueId);
                result.setSuccess(false);
                result.setMessage("Issue不存在");
                return result;
            }
            
            Issue issue = existingIssue.get();
            issue.setStatus("opened");
            issue.setClosedAt(null);
            issue.setResolutionAt(null);
            issue.setResolutionTimeMinutes(null);
            issue.setUpdatedAt(parseDateTime(attrs.getUpdatedAt()));
            
            Issue savedIssue = issueRepository.save(issue);
            
            result.setSuccess(true);
            result.setMessage("Issue重新打开成功");
            result.setIssue(savedIssue);
            
            logger.info("成功重新打开Issue: issueId={}", issueId);
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理Issue重新打开事件失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("重新打开失败: " + e.getMessage());
            return result;
        }
    }    

    /**
     * 分析Issue类型和严重程度
     */
    private void analyzeIssueTypeAndSeverity(Issue issue, IssueEventRequest.IssueAttributes attrs) {
        try {
            String title = attrs.getTitle().toLowerCase();
            String description = attrs.getDescription() != null ? attrs.getDescription().toLowerCase() : "";
            String[] labels = attrs.getLabels();
            
            // 分析Issue类型
            String issueType = determineIssueType(title, description, labels);
            issue.setIssueType(issueType);
            
            // 分析优先级
            String priority = determinePriority(title, description, labels);
            issue.setPriority(priority);
            
            // 分析严重程度
            String severity = determineSeverity(title, description, labels);
            issue.setSeverity(severity);
            
            logger.debug("Issue分析结果: type={}, priority={}, severity={}", 
                issueType, priority, severity);
            
        } catch (Exception e) {
            logger.error("分析Issue类型和严重程度失败: {}", e.getMessage(), e);
            // 设置默认值
            issue.setIssueType("task");
            issue.setPriority("medium");
            issue.setSeverity("minor");
        }
    }
    
    /**
     * 确定Issue类型
     */
    private String determineIssueType(String title, String description, String[] labels) {
        // 检查标签
        if (labels != null) {
            for (String label : labels) {
                String lowerLabel = label.toLowerCase();
                if (lowerLabel.contains("bug") || lowerLabel.contains("defect")) {
                    return "bug";
                } else if (lowerLabel.contains("feature") || lowerLabel.contains("enhancement")) {
                    return "feature";
                } else if (lowerLabel.contains("task") || lowerLabel.contains("chore")) {
                    return "task";
                }
            }
        }
        
        // 检查标题和描述中的关键词
        String content = title + " " + description;
        if (content.contains("bug") || content.contains("error") || content.contains("issue") || 
            content.contains("problem") || content.contains("fail") || content.contains("broken")) {
            return "bug";
        } else if (content.contains("feature") || content.contains("enhancement") || 
                   content.contains("improve") || content.contains("add")) {
            return "feature";
        } else {
            return "task";
        }
    }
    
    /**
     * 确定优先级
     */
    private String determinePriority(String title, String description, String[] labels) {
        // 检查标签
        if (labels != null) {
            for (String label : labels) {
                String lowerLabel = label.toLowerCase();
                if (lowerLabel.contains("critical") || lowerLabel.contains("urgent")) {
                    return "critical";
                } else if (lowerLabel.contains("high")) {
                    return "high";
                } else if (lowerLabel.contains("low")) {
                    return "low";
                }
            }
        }
        
        // 检查标题中的关键词
        String content = title + " " + description;
        if (content.contains("critical") || content.contains("urgent") || content.contains("asap")) {
            return "critical";
        } else if (content.contains("high") || content.contains("important")) {
            return "high";
        } else if (content.contains("low") || content.contains("minor")) {
            return "low";
        } else {
            return "medium";
        }
    }
    
    /**
     * 确定严重程度
     */
    private String determineSeverity(String title, String description, String[] labels) {
        // 检查标签
        if (labels != null) {
            for (String label : labels) {
                String lowerLabel = label.toLowerCase();
                if (lowerLabel.contains("blocker")) {
                    return "blocker";
                } else if (lowerLabel.contains("critical")) {
                    return "critical";
                } else if (lowerLabel.contains("major")) {
                    return "major";
                } else if (lowerLabel.contains("minor")) {
                    return "minor";
                }
            }
        }
        
        // 检查标题和描述中的关键词
        String content = title + " " + description;
        if (content.contains("crash") || content.contains("data loss") || content.contains("security")) {
            return "blocker";
        } else if (content.contains("critical") || content.contains("severe")) {
            return "critical";
        } else if (content.contains("major") || content.contains("significant")) {
            return "major";
        } else {
            return "minor";
        }
    }
    
    /**
     * 计算响应时间
     */
    private void calculateResponseTime(Issue issue) {
        if (issue.getCreatedAt() != null && issue.getFirstResponseAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(issue.getCreatedAt(), issue.getFirstResponseAt());
            issue.setResponseTimeMinutes(minutes);
        }
    }
    
    /**
     * 计算解决时间
     */
    private void calculateResolutionTime(Issue issue) {
        if (issue.getCreatedAt() != null && issue.getResolutionAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(issue.getCreatedAt(), issue.getResolutionAt());
            issue.setResolutionTimeMinutes(minutes);
        }
    }
    
    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (!StringUtils.hasText(dateTimeStr)) {
            return LocalDateTime.now();
        }
        
        try {
            // GitLab通常使用ISO 8601格式
            if (dateTimeStr.endsWith("Z")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.length() - 1);
            }
            
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (Exception ignored) {
                    // 尝试下一个格式
                }
            }
            
            logger.warn("无法解析时间字符串: {}, 使用当前时间", dateTimeStr);
            return LocalDateTime.now();
            
        } catch (Exception e) {
            logger.warn("解析时间字符串失败: {}, 使用当前时间", dateTimeStr, e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 获取Bug修复效率统计
     */
    public BugFixEfficiencyStats getBugFixEfficiencyStats(String projectId, LocalDateTime start, LocalDateTime end) {
        try {
            logger.info("获取Bug修复效率统计: projectId={}, start={}, end={}", projectId, start, end);
            
            // 委托给BugFixEfficiencyService处理
            return bugFixEfficiencyService.calculateBugFixEfficiency(projectId, null, start, end);
            
        } catch (Exception e) {
            logger.error("获取Bug修复效率统计失败: projectId={}, error={}", projectId, e.getMessage(), e);
            throw new RuntimeException("获取Bug修复效率统计失败", e);
        }
    }
    
    /**
     * 获取长时间未解决的Issue
     */
    public List<Issue> getLongPendingIssues(String projectId, Integer hoursThreshold) {
        try {
            logger.info("获取长时间未解决的Issue: projectId={}, threshold={}小时", projectId, hoursThreshold);
            
            List<Issue> longPendingIssues;
            if (StringUtils.hasText(projectId)) {
                longPendingIssues = issueRepository.findLongPendingIssuesByProject(projectId, hoursThreshold);
            } else {
                longPendingIssues = issueRepository.findLongPendingIssues(hoursThreshold);
            }
            
            logger.info("找到{}个长时间未解决的Issue", longPendingIssues.size());
            return longPendingIssues;
            
        } catch (Exception e) {
            logger.error("获取长时间未解决的Issue失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Issue处理结果类
     */
    public static class IssueProcessResult {
        private String projectId;
        private String issueId;
        private String action;
        private LocalDateTime processTime;
        private boolean success;
        private String message;
        private Issue issue;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getIssueId() { return issueId; }
        public void setIssueId(String issueId) { this.issueId = issueId; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public LocalDateTime getProcessTime() { return processTime; }
        public void setProcessTime(LocalDateTime processTime) { this.processTime = processTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Issue getIssue() { return issue; }
        public void setIssue(Issue issue) { this.issue = issue; }
    }
    

}