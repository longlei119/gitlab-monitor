package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 测试覆盖率实体类
 * 记录单元测试覆盖率相关指标
 */
@Entity
@Table(name = "test_coverage", indexes = {
    @Index(name = "idx_coverage_project", columnList = "projectId"),
    @Index(name = "idx_coverage_commit", columnList = "commitSha"),
    @Index(name = "idx_coverage_timestamp", columnList = "timestamp")
})
public class TestCoverage {
    
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
    
    @Column(precision = 5, scale = 2)
    private Double lineCoverage;
    
    @Column(precision = 5, scale = 2)
    private Double branchCoverage;
    
    @Column(precision = 5, scale = 2)
    private Double functionCoverage;
    
    @Column
    private Integer totalLines;
    
    @Column
    private Integer coveredLines;
    
    @Column
    private Integer totalBranches;
    
    @Column
    private Integer coveredBranches;
    
    @Column
    private Integer totalFunctions;
    
    @Column
    private Integer coveredFunctions;
    
    @Column
    private Integer totalClasses;
    
    @Column
    private Integer coveredClasses;
    
    @Column(length = 50)
    private String reportType; // jacoco, cobertura, lcov, etc.
    
    @Column(columnDefinition = "TEXT")
    private String reportPath;
    
    @Column(length = 20)
    private String status; // PASSED, FAILED, WARNING
    
    @Column(precision = 5, scale = 2)
    private Double threshold; // minimum required coverage
    
    // 默认构造函数
    public TestCoverage() {}
    
    // 构造函数
    public TestCoverage(String projectId, String commitSha, LocalDateTime timestamp) {
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
    
    public Double getLineCoverage() {
        return lineCoverage;
    }
    
    public void setLineCoverage(Double lineCoverage) {
        this.lineCoverage = lineCoverage;
    }
    
    public Double getBranchCoverage() {
        return branchCoverage;
    }
    
    public void setBranchCoverage(Double branchCoverage) {
        this.branchCoverage = branchCoverage;
    }
    
    public Double getFunctionCoverage() {
        return functionCoverage;
    }
    
    public void setFunctionCoverage(Double functionCoverage) {
        this.functionCoverage = functionCoverage;
    }
    
    public Integer getTotalLines() {
        return totalLines;
    }
    
    public void setTotalLines(Integer totalLines) {
        this.totalLines = totalLines;
    }
    
    public Integer getCoveredLines() {
        return coveredLines;
    }
    
    public void setCoveredLines(Integer coveredLines) {
        this.coveredLines = coveredLines;
    }
    
    public Integer getTotalBranches() {
        return totalBranches;
    }
    
    public void setTotalBranches(Integer totalBranches) {
        this.totalBranches = totalBranches;
    }
    
    public Integer getCoveredBranches() {
        return coveredBranches;
    }
    
    public void setCoveredBranches(Integer coveredBranches) {
        this.coveredBranches = coveredBranches;
    }
    
    public Integer getTotalFunctions() {
        return totalFunctions;
    }
    
    public void setTotalFunctions(Integer totalFunctions) {
        this.totalFunctions = totalFunctions;
    }
    
    public Integer getCoveredFunctions() {
        return coveredFunctions;
    }
    
    public void setCoveredFunctions(Integer coveredFunctions) {
        this.coveredFunctions = coveredFunctions;
    }
    
    public Integer getTotalClasses() {
        return totalClasses;
    }
    
    public void setTotalClasses(Integer totalClasses) {
        this.totalClasses = totalClasses;
    }
    
    public Integer getCoveredClasses() {
        return coveredClasses;
    }
    
    public void setCoveredClasses(Integer coveredClasses) {
        this.coveredClasses = coveredClasses;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getReportPath() {
        return reportPath;
    }
    
    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public String toString() {
        return "TestCoverage{" +
                "id=" + id +
                ", projectId='" + projectId + '\'' +
                ", commitSha='" + commitSha + '\'' +
                ", timestamp=" + timestamp +
                ", lineCoverage=" + lineCoverage +
                ", branchCoverage=" + branchCoverage +
                ", functionCoverage=" + functionCoverage +
                ", totalLines=" + totalLines +
                ", coveredLines=" + coveredLines +
                ", status='" + status + '\'' +
                '}';
    }
}