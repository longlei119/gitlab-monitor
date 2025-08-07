package com.gitlab.metrics.service;

import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.service.SecurityAnalysisService.QualityThresholdResult;
import com.gitlab.metrics.service.SecurityAnalysisService.SecurityAnalysisResult;
import com.gitlab.metrics.service.SecurityAnalysisService.PerformanceAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警服务
 * 负责处理安全和性能问题的告警通知
 */
@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    // 告警级别
    public enum AlertLevel {
        CRITICAL("CRITICAL", 1),
        HIGH("HIGH", 2),
        MEDIUM("MEDIUM", 3),
        LOW("LOW", 4),
        INFO("INFO", 5);
        
        private final String level;
        private final int priority;
        
        AlertLevel(String level, int priority) {
            this.level = level;
            this.priority = priority;
        }
        
        public String getLevel() {
            return level;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    // 告警类型
    public enum AlertType {
        SECURITY_VULNERABILITY("安全漏洞"),
        PERFORMANCE_ISSUE("性能问题"),
        QUALITY_GATE_FAILURE("质量门禁失败"),
        THRESHOLD_VIOLATION("阈值违规"),
        MERGE_BLOCKED("合并阻止"),
        BUG_FIX_TIMEOUT("Bug修复超时"),
        BUG_RESPONSE_TIMEOUT("Bug响应超时"),
        BUG_TIMEOUT("Bug超时");
        
        private final String description;
        
        AlertType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理安全分析结果告警
     * 
     * @param result 安全分析结果
     */
    public void handleSecurityAnalysisAlert(SecurityAnalysisResult result) {
        try {
            logger.info("处理安全分析告警: projectId={}, 漏洞数量={}", 
                result.getProjectId(), result.getTotalVulnerabilities());
            
            List<Alert> alerts = new ArrayList<>();
            
            // 根据漏洞数量和严重程度生成告警
            if (result.getTotalVulnerabilities() > 0) {
                AlertLevel level = determineSecurityAlertLevel(result);
                
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.SECURITY_VULNERABILITY);
                alert.setLevel(level);
                alert.setTitle("发现安全漏洞");
                alert.setMessage(String.format("项目 %s 发现 %d 个安全漏洞，风险评分: %.2f", 
                    result.getProjectId(), result.getTotalVulnerabilities(), result.getRiskScore()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(result);
                
                alerts.add(alert);
            }
            
            // 发送告警
            for (Alert alert : alerts) {
                sendAlert(alert);
            }
            
        } catch (Exception e) {
            logger.error("处理安全分析告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理性能分析结果告警
     * 
     * @param result 性能分析结果
     */
    public void handlePerformanceAnalysisAlert(PerformanceAnalysisResult result) {
        try {
            logger.info("处理性能分析告警: projectId={}, 问题数量={}", 
                result.getProjectId(), result.getTotalIssues());
            
            List<Alert> alerts = new ArrayList<>();
            
            // 根据性能问题数量和复杂度生成告警
            if (result.getTotalIssues() > 10 || result.getComplexityScore() > 15.0) {
                AlertLevel level = determinePerformanceAlertLevel(result);
                
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.PERFORMANCE_ISSUE);
                alert.setLevel(level);
                alert.setTitle("发现性能问题");
                alert.setMessage(String.format("项目 %s 发现 %d 个性能问题，复杂度评分: %.2f", 
                    result.getProjectId(), result.getTotalIssues(), result.getComplexityScore()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(result);
                
                alerts.add(alert);
            }
            
            // 发送告警
            for (Alert alert : alerts) {
                sendAlert(alert);
            }
            
        } catch (Exception e) {
            logger.error("处理性能分析告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理质量阈值检查告警
     * 
     * @param result 质量阈值检查结果
     */
    public void handleQualityThresholdAlert(QualityThresholdResult result) {
        try {
            logger.info("处理质量阈值告警: projectId={}, 是否阻止合并={}", 
                result.getProjectId(), result.isShouldBlockMerge());
            
            List<Alert> alerts = new ArrayList<>();
            
            // 如果需要阻止合并，生成高优先级告警
            if (result.isShouldBlockMerge()) {
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.MERGE_BLOCKED);
                alert.setLevel(AlertLevel.CRITICAL);
                alert.setTitle("合并被阻止");
                alert.setMessage(String.format("项目 %s 由于质量问题被阻止合并", result.getProjectId()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(result);
                
                alerts.add(alert);
            }
            
            // 为每个阈值违规生成告警
            result.getSecurityViolations().forEach(violation -> {
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.THRESHOLD_VIOLATION);
                alert.setLevel(AlertLevel.valueOf(violation.getSeverity()));
                alert.setTitle("安全阈值违规");
                alert.setMessage(String.format("%s: 实际值 %s，期望值 %s", 
                    violation.getDescription(), violation.getActualValue(), violation.getExpectedValue()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(violation);
                
                alerts.add(alert);
            });
            
            result.getPerformanceViolations().forEach(violation -> {
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.THRESHOLD_VIOLATION);
                alert.setLevel(AlertLevel.valueOf(violation.getSeverity()));
                alert.setTitle("性能阈值违规");
                alert.setMessage(String.format("%s: 实际值 %s，期望值 %s", 
                    violation.getDescription(), violation.getActualValue(), violation.getExpectedValue()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(violation);
                
                alerts.add(alert);
            });
            
            result.getQualityGateViolations().forEach(violation -> {
                Alert alert = new Alert();
                alert.setProjectId(result.getProjectId());
                alert.setType(AlertType.QUALITY_GATE_FAILURE);
                alert.setLevel(AlertLevel.valueOf(violation.getSeverity()));
                alert.setTitle("质量门禁失败");
                alert.setMessage(String.format("%s: 实际值 %s，期望值 %s", 
                    violation.getDescription(), violation.getActualValue(), violation.getExpectedValue()));
                alert.setTimestamp(LocalDateTime.now());
                alert.setDetails(violation);
                
                alerts.add(alert);
            });
            
            // 发送告警
            for (Alert alert : alerts) {
                sendAlert(alert);
            }
            
        } catch (Exception e) {
            logger.error("处理质量阈值告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送告警消息（公共方法）
     * 
     * @param alert 告警信息
     */
    public void sendAlert(Alert alert) {
        sendAlertInternal(alert);
    }
    
    /**
     * 发送覆盖率相关告警
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param alertType 告警类型
     * @param message 告警消息
     */
    public void sendCoverageAlert(String projectId, String commitSha, String alertType, String message) {
        try {
            Alert alert = new Alert();
            alert.setProjectId(projectId);
            alert.setRelatedEntityId(commitSha);
            alert.setType(AlertType.QUALITY_GATE_FAILURE);
            alert.setLevel(AlertLevel.HIGH);
            alert.setTitle("覆盖率质量门禁告警");
            alert.setMessage(message);
            alert.setTimestamp(LocalDateTime.now());
            
            sendAlert(alert);
            
            logger.info("成功发送覆盖率告警: projectId={}, commitSha={}, alertType={}", 
                projectId, commitSha, alertType);
            
        } catch (Exception e) {
            logger.error("发送覆盖率告警失败: projectId={}, commitSha={}, error={}", 
                projectId, commitSha, e.getMessage(), e);
        }
    }
    
    /**
     * 发送告警消息（内部方法）
     * 
     * @param alert 告警信息
     */
    private void sendAlertInternal(Alert alert) {
        try {
            String alertJson = objectMapper.writeValueAsString(alert);
            
            // 发送到告警队列
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.GITLAB_EVENTS_EXCHANGE,
                "alert.notification",
                alertJson
            );
            
            logger.info("成功发送告警: projectId={}, type={}, level={}", 
                alert.getProjectId(), alert.getType(), alert.getLevel());
            
        } catch (Exception e) {
            logger.error("发送告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 确定安全告警级别
     */
    private AlertLevel determineSecurityAlertLevel(SecurityAnalysisResult result) {
        if (result.getRiskScore() > 20.0) {
            return AlertLevel.CRITICAL;
        } else if (result.getRiskScore() > 10.0) {
            return AlertLevel.HIGH;
        } else if (result.getRiskScore() > 5.0) {
            return AlertLevel.MEDIUM;
        } else {
            return AlertLevel.LOW;
        }
    }
    
    /**
     * 确定性能告警级别
     */
    private AlertLevel determinePerformanceAlertLevel(PerformanceAnalysisResult result) {
        if (result.getRiskScore() > 30.0 || result.getComplexityScore() > 20.0) {
            return AlertLevel.HIGH;
        } else if (result.getRiskScore() > 15.0 || result.getComplexityScore() > 15.0) {
            return AlertLevel.MEDIUM;
        } else {
            return AlertLevel.LOW;
        }
    }
    
    /**
     * 告警信息类
     */
    public static class Alert {
        private String projectId;
        private String assigneeId;
        private String relatedEntityId;
        private String issueId;
        private AlertType type;
        private AlertLevel level;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private LocalDateTime createdAt;
        private Object details;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getAssigneeId() { return assigneeId; }
        public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
        
        public String getRelatedEntityId() { return relatedEntityId; }
        public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }
        
        public String getIssueId() { return issueId; }
        public void setIssueId(String issueId) { this.issueId = issueId; }
        
        public AlertType getType() { return type; }
        public void setType(AlertType type) { this.type = type; }
        
        public AlertLevel getLevel() { return level; }
        public void setLevel(AlertLevel level) { this.level = level; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
        
        @Override
        public String toString() {
            return "Alert{" +
                    "projectId='" + projectId + '\'' +
                    ", assigneeId='" + assigneeId + '\'' +
                    ", relatedEntityId='" + relatedEntityId + '\'' +
                    ", type=" + type +
                    ", level=" + level +
                    ", title='" + title + '\'' +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }
}