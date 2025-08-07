package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合并请求实体类
 * 记录GitLab合并请求的相关信息，包括创建时间、合并时间、状态等
 */
@Entity
@Table(name = "merge_requests", indexes = {
    @Index(name = "idx_mr_project", columnList = "projectId"),
    @Index(name = "idx_mr_author", columnList = "authorId"),
    @Index(name = "idx_mr_status", columnList = "status"),
    @Index(name = "idx_mr_created", columnList = "createdAt")
})
public class MergeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String mrId; // GitLab MR ID
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String projectId;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String authorId;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String authorName;
    
    @Column(nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime mergedAt;
    
    @Column
    private LocalDateTime closedAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false, length = 20)
    @NotNull
    private String status; // opened, merged, closed
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String sourceBranch;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String targetBranch;
    
    @Column(columnDefinition = "TEXT")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private Integer changedFiles;
    
    @Column
    private Integer additions;
    
    @Column
    private Integer deletions;
    
    @Column
    private Integer commits;
    
    @Column(length = 100)
    private String assigneeId;
    
    @Column(length = 255)
    private String assigneeName;
    
    @Column(length = 100)
    private String mergedById;
    
    @Column(length = 255)
    private String mergedByName;
    
    @Column
    private Boolean workInProgress;
    
    @Column
    private Boolean squash;
    
    @Column(columnDefinition = "TEXT")
    private String webUrl;
    
    @OneToMany(mappedBy = "mergeRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CodeReview> reviews;
    
    // 默认构造函数
    public MergeRequest() {}
    
    // 构造函数
    public MergeRequest(String mrId, String projectId, String authorId, String authorName, LocalDateTime createdAt, String status, String sourceBranch, String targetBranch) {
        this.mrId = mrId;
        this.projectId = projectId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.status = status;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMrId() {
        return mrId;
    }
    
    public void setMrId(String mrId) {
        this.mrId = mrId;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getMergedAt() {
        return mergedAt;
    }
    
    public void setMergedAt(LocalDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }
    
    public LocalDateTime getClosedAt() {
        return closedAt;
    }
    
    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSourceBranch() {
        return sourceBranch;
    }
    
    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }
    
    public String getTargetBranch() {
        return targetBranch;
    }
    
    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
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
    
    public Integer getChangedFiles() {
        return changedFiles;
    }
    
    public void setChangedFiles(Integer changedFiles) {
        this.changedFiles = changedFiles;
    }
    
    public Integer getAdditions() {
        return additions;
    }
    
    public void setAdditions(Integer additions) {
        this.additions = additions;
    }
    
    public Integer getDeletions() {
        return deletions;
    }
    
    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }
    
    public Integer getCommits() {
        return commits;
    }
    
    public void setCommits(Integer commits) {
        this.commits = commits;
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
    
    public String getMergedById() {
        return mergedById;
    }
    
    public void setMergedById(String mergedById) {
        this.mergedById = mergedById;
    }
    
    public String getMergedByName() {
        return mergedByName;
    }
    
    public void setMergedByName(String mergedByName) {
        this.mergedByName = mergedByName;
    }
    
    public Boolean getWorkInProgress() {
        return workInProgress;
    }
    
    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }
    
    public Boolean getSquash() {
        return squash;
    }
    
    public void setSquash(Boolean squash) {
        this.squash = squash;
    }
    
    public String getWebUrl() {
        return webUrl;
    }
    
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
    
    public List<CodeReview> getReviews() {
        return reviews;
    }
    
    public void setReviews(List<CodeReview> reviews) {
        this.reviews = reviews;
    }
    
    @Override
    public String toString() {
        return "MergeRequest{" +
                "id=" + id +
                ", mrId='" + mrId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", createdAt=" + createdAt +
                ", mergedAt=" + mergedAt +
                ", status='" + status + '\'' +
                ", sourceBranch='" + sourceBranch + '\'' +
                ", targetBranch='" + targetBranch + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}