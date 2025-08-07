package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 代码质量指标实体类
 * 记录代码质量分析结果，包括复杂度、重复率、可维护性等指标
 */
@Entity
@Table(name = "quality_metrics", indexes = {
    @Index(name = "idx_quality_project", columnList = "projectId"),
    @Index(name = "idx_quality_commit", columnList = "commitSha"),
    @Index(name = "idx_quality_timestamp", columnList = "timestamp")
})
public class QualityMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String projectId;
    
    @Column(nullable = false, length = 40)
    @NotNull
    private String commitSha;
    
    @Column(nullable = false)
    @NotNull
    private LocalDateTime timestamp;
    
    @Column(precision = 10, scale = 2)
    private Double codeComplexity;
    
    @Column(precision = 5, scale = 2)
    private Double duplicateRate;
    
    @Column(precision = 5, scale = 2)
    private Double maintainabilityIndex;
    
    @Column
    private Integer securityIssues;
    
    @Column
    private Integer performanceIssues;
    
    @Column
    private Integer codeSmells;
    
    @Column
    private Integer bugs;
    
    @Column
    private Integer vulnerabilities;
    
    @Column
    private Integer hotspots;
    
    @Column(precision = 5, scale = 2)
    private Double technicalDebt; // in hours
    
    @Column(length = 20)
    private String qualityGate; // PASSED, FAILED, ERROR
    
    @Column(columnDefinition = "TEXT")
    private String analysisDetails;
    
    // 默认构造函数
    public QualityMetrics() {}
    
    // 构造函数
    public QualityMetrics(String projectId, String commitSha, LocalDateTime timestamp) {
        this.projectId = projectId;
        this.commitSha = commitSha;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getCommitSha() {
        return commitSha;
    }
    
    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Double getCodeComplexity() {
        return codeComplexity;
    }
    
    public void setCodeComplexity(Double codeComplexity) {
        this.codeComplexity = codeComplexity;
    }
    
    public Double getDuplicateRate() {
        return duplicateRate;
    }
    
    public void setDuplicateRate(Double duplicateRate) {
        this.duplicateRate = duplicateRate;
    }
    
    public Double getMaintainabilityIndex() {
        return maintainabilityIndex;
    }
    
    public void setMaintainabilityIndex(Double maintainabilityIndex) {
        this.maintainabilityIndex = maintainabilityIndex;
    }
    
    public Integer getSecurityIssues() {
        return securityIssues;
    }
    
    public void setSecurityIssues(Integer securityIssues) {
        this.securityIssues = securityIssues;
    }
    
    public Integer getPerformanceIssues() {
        return performanceIssues;
    }
    
    public void setPerformanceIssues(Integer performanceIssues) {
        this.performanceIssues = performanceIssues;
    }
    
    public Integer getCodeSmells() {
        return codeSmells;
    }
    
    public void setCodeSmells(Integer codeSmells) {
        this.codeSmells = codeSmells;
    }
    
    public Integer getBugs() {
        return bugs;
    }
    
    public void setBugs(Integer bugs) {
        this.bugs = bugs;
    }
    
    public Integer getVulnerabilities() {
        return vulnerabilities;
    }
    
    public void setVulnerabilities(Integer vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
    
    public Integer getHotspots() {
        return hotspots;
    }
    
    public void setHotspots(Integer hotspots) {
        this.hotspots = hotspots;
    }
    
    public Double getTechnicalDebt() {
        return technicalDebt;
    }
    
    public void setTechnicalDebt(Double technicalDebt) {
        this.technicalDebt = technicalDebt;
    }
    
    public String getQualityGate() {
        return qualityGate;
    }
    
    public void setQualityGate(String qualityGate) {
        this.qualityGate = qualityGate;
    }
    
    public String getAnalysisDetails() {
        return analysisDetails;
    }
    
    public void setAnalysisDetails(String analysisDetails) {
        this.analysisDetails = analysisDetails;
    }
    
    @Override
    public String toString() {
        return "QualityMetrics{" +
                "id=" + id +
                ", projectId='" + projectId + '\'' +
                ", commitSha='" + commitSha + '\'' +
                ", timestamp=" + timestamp +
                ", codeComplexity=" + codeComplexity +
                ", duplicateRate=" + duplicateRate +
                ", maintainabilityIndex=" + maintainabilityIndex +
                ", securityIssues=" + securityIssues +
                ", performanceIssues=" + performanceIssues +
                ", codeSmells=" + codeSmells +
                ", qualityGate='" + qualityGate + '\'' +
                '}';
    }
}