package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码提交实体类
 * 记录开发者的代码提交信息，包括提交SHA、项目ID、开发者信息、时间戳等
 */
@Entity
@Table(name = "commits", indexes = {
    @Index(name = "idx_commit_project_developer", columnList = "projectId,developerId"),
    @Index(name = "idx_commit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_commit_sha", columnList = "commitSha")
})
public class Commit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 40)
    @NotNull
    private String commitSha;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String projectId;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String developerId;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String developerName;
    
    @Column(nullable = false)
    @NotNull
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(length = 255)
    private String branch;
    
    @Column
    private Integer linesAdded;
    
    @Column
    private Integer linesDeleted;
    
    @Column
    private Integer filesChanged;
    
    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileChange> fileChanges;
    
    // 默认构造函数
    public Commit() {}
    
    // 构造函数
    public Commit(String commitSha, String projectId, String developerId, String developerName, LocalDateTime timestamp) {
        this.commitSha = commitSha;
        this.projectId = projectId;
        this.developerId = developerId;
        this.developerName = developerName;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCommitSha() {
        return commitSha;
    }
    
    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getDeveloperId() {
        return developerId;
    }
    
    public void setDeveloperId(String developerId) {
        this.developerId = developerId;
    }
    
    public String getDeveloperName() {
        return developerName;
    }
    
    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public Integer getLinesAdded() {
        return linesAdded;
    }
    
    public void setLinesAdded(Integer linesAdded) {
        this.linesAdded = linesAdded;
    }
    
    public Integer getLinesDeleted() {
        return linesDeleted;
    }
    
    public void setLinesDeleted(Integer linesDeleted) {
        this.linesDeleted = linesDeleted;
    }
    
    public Integer getFilesChanged() {
        return filesChanged;
    }
    
    public void setFilesChanged(Integer filesChanged) {
        this.filesChanged = filesChanged;
    }
    
    public List<FileChange> getFileChanges() {
        return fileChanges;
    }
    
    public void setFileChanges(List<FileChange> fileChanges) {
        this.fileChanges = fileChanges;
    }
    
    @Override
    public String toString() {
        return "Commit{" +
                "id=" + id +
                ", commitSha='" + commitSha + '\'' +
                ", projectId='" + projectId + '\'' +
                ", developerId='" + developerId + '\'' +
                ", developerName='" + developerName + '\'' +
                ", timestamp=" + timestamp +
                ", branch='" + branch + '\'' +
                ", linesAdded=" + linesAdded +
                ", linesDeleted=" + linesDeleted +
                ", filesChanged=" + filesChanged +
                '}';
    }
}