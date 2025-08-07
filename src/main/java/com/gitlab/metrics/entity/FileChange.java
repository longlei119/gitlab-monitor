package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * 文件变更实体类
 * 记录每次提交中具体文件的变更信息
 */
@Entity
@Table(name = "file_changes", indexes = {
    @Index(name = "idx_file_change_commit", columnList = "commit_id"),
    @Index(name = "idx_file_change_path", columnList = "filePath")
})
public class FileChange {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    @NotNull
    private Commit commit;
    
    @Column(nullable = false, length = 500)
    @NotNull
    private String filePath;
    
    @Column(length = 20)
    private String changeType; // added, modified, deleted, renamed
    
    @Column
    private Integer linesAdded;
    
    @Column
    private Integer linesDeleted;
    
    @Column(length = 500)
    private String oldPath; // for renamed files
    
    // 默认构造函数
    public FileChange() {}
    
    // 构造函数
    public FileChange(Commit commit, String filePath, String changeType) {
        this.commit = commit;
        this.filePath = filePath;
        this.changeType = changeType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Commit getCommit() {
        return commit;
    }
    
    public void setCommit(Commit commit) {
        this.commit = commit;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
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
    
    public String getOldPath() {
        return oldPath;
    }
    
    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }
    
    @Override
    public String toString() {
        return "FileChange{" +
                "id=" + id +
                ", filePath='" + filePath + '\'' +
                ", changeType='" + changeType + '\'' +
                ", linesAdded=" + linesAdded +
                ", linesDeleted=" + linesDeleted +
                ", oldPath='" + oldPath + '\'' +
                '}';
    }
}