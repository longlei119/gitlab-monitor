package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 问题（Issue）实体类
 * 记录GitLab Issue信息，用于Bug修复效率跟踪
 */
@Entity
@Table(name = "issues", indexes = {
    @Index(name = "idx_issue_project", columnList = "projectId"),
    @Index(name = "idx_issue_assignee", columnList = "assigneeId"),
    @Index(name = "idx_issue_status", columnList = "status"),
    @Index(name = "idx_issue_created", columnList = "createdAt"),
    @Index(name = "idx_issue_type", columnList = "issueType")
})
public class Issue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String issueId; // GitLab Issue ID
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String projectId;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String authorId;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String authorName;
    
    @Column(length = 100)
    private String assigneeId;
    
    @Column(length = 255)
    private String assigneeName;
    
    @Column(nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime closedAt;
    
    @Column
    private LocalDateTime dueDate;
    
    @Column(nullable = false, length = 20)
    @NotNull
    private String status; // opened, closed, reopened
    
    @Column(length = 30)
    private String issueType; // bug, feature, enhancement, task
    
    @Column(length = 20)
    private String priority; // low, medium, high, critical
    
    @Column(length = 20)
    private String severity; // minor, major, critical, blocker
    
    @Column(columnDefinition = "TEXT")
    private String labels;
    
    @Column
    private Integer weight;
    
    @Column
    private Integer timeEstimate; // in seconds
    
    @Column
    private Integer timeSpent; // in seconds
    
    @Column(length = 100)
    private String milestoneId;
    
    @Column(length = 255)
    private String milestoneTitle;
    
    @Column(columnDefinition = "TEXT")
    private String webUrl;
    
    @Column
    private Integer upvotes;
    
    @Column
    private Integer downvotes;
    
    @Column
    private Boolean confidential;
    
    @Column
    private LocalDateTime firstResponseAt; // 首次响应时间
    
    @Column
    private LocalDateTime resolutionAt; // 解决时间
    
    @Column
    private Long responseTimeMinutes; // 响应时间（分钟）
    
    @Column
    private Long resolutionTimeMinutes; // 解决时间（分钟）
    
    // 默认构造函数
    public Issue() {}
    
    // 构造函数
    public Issue(String issueId, String projectId, String title, String authorId, String authorName, LocalDateTime createdAt, String status) {
        this.issueId = issueId;
        this.projectId = projectId;
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIssueId() {
        return issueId;
    }
    
    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAssigneeId() {
        return assigneeId;
    }
    
    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }
    
    public String getAssigneeName() {
        return assigneeName;
    }
    
    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getClosedAt() {
        return closedAt;
    }
    
    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getIssueType() {
        return issueType;
    }
    
    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getLabels() {
        return labels;
    }
    
    public void setLabels(String labels) {
        this.labels = labels;
    }
    
    public Integer getWeight() {
        return weight;
    }
    
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    public Integer getTimeEstimate() {
        return timeEstimate;
    }
    
    public void setTimeEstimate(Integer timeEstimate) {
        this.timeEstimate = timeEstimate;
    }
    
    public Integer getTimeSpent() {
        return timeSpent;
    }
    
    public void setTimeSpent(Integer timeSpent) {
        this.timeSpent = timeSpent;
    }
    
    public String getMilestoneId() {
        return milestoneId;
    }
    
    public void setMilestoneId(String milestoneId) {
        this.milestoneId = milestoneId;
    }
    
    public String getMilestoneTitle() {
        return milestoneTitle;
    }
    
    public void setMilestoneTitle(String milestoneTitle) {
        this.milestoneTitle = milestoneTitle;
    }
    
    public String getWebUrl() {
        return webUrl;
    }
    
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
    
    public Integer getUpvotes() {
        return upvotes;
    }
    
    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }
    
    public Integer getDownvotes() {
        return downvotes;
    }
    
    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }
    
    public Boolean getConfidential() {
        return confidential;
    }
    
    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }
    
    public LocalDateTime getFirstResponseAt() {
        return firstResponseAt;
    }
    
    public void setFirstResponseAt(LocalDateTime firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }
    
    public LocalDateTime getResolutionAt() {
        return resolutionAt;
    }
    
    public void setResolutionAt(LocalDateTime resolutionAt) {
        this.resolutionAt = resolutionAt;
    }
    
    public Long getResponseTimeMinutes() {
        return responseTimeMinutes;
    }
    
    public void setResponseTimeMinutes(Long responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }
    
    public Long getResolutionTimeMinutes() {
        return resolutionTimeMinutes;
    }
    
    public void setResolutionTimeMinutes(Long resolutionTimeMinutes) {
        this.resolutionTimeMinutes = resolutionTimeMinutes;
    }
    
    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", issueId='" + issueId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", title='" + title + '\'' +
                ", authorId='" + authorId + '\'' +
                ", assigneeId='" + assigneeId + '\'' +
                ", createdAt=" + createdAt +
                ", closedAt=" + closedAt +
                ", status='" + status + '\'' +
                ", issueType='" + issueType + '\'' +
                ", priority='" + priority + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}