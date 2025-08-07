package com.gitlab.metrics.dto;

import com.gitlab.metrics.entity.QualityMetrics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码质量指标响应DTO
 * 用于返回代码质量指标API的响应数据
 */
public class QualityMetricsResponse {
    
    private String projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timeRange;
    private List<QualityMetrics> qualityMetrics;
    private QualityTrendData trendData;
    private QualityOverview overview;
    
    public QualityMetricsResponse() {}
    
    public QualityMetricsResponse(String projectId, String timeRange) {
        this.projectId = projectId;
        this.timeRange = timeRange;
    }
    
    // Getters and Setters
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
    
    public List<QualityMetrics> getQualityMetrics() {
        return qualityMetrics;
    }
    
    public void setQualityMetrics(List<QualityMetrics> qualityMetrics) {
        this.qualityMetrics = qualityMetrics;
    }
    
    public QualityTrendData getTrendData() {
        return trendData;
    }
    
    public void setTrendData(QualityTrendData trendData) {
        this.trendData = trendData;
    }
    
    public QualityOverview getOverview() {
        return overview;
    }
    
    public void setOverview(QualityOverview overview) {
        this.overview = overview;
    }
    
    /**
     * 质量趋势数据
     */
    public static class QualityTrendData {
        private Double averageComplexity;
        private Double averageDuplicateRate;
        private Double averageMaintainabilityIndex;
        private Double averageTechnicalDebt;
        private List<QualityTrendPoint> trendPoints;
        
        // Getters and Setters
        public Double getAverageComplexity() {
            return averageComplexity;
        }
        
        public void setAverageComplexity(Double averageComplexity) {
            this.averageComplexity = averageComplexity;
        }
        
        public Double getAverageDuplicateRate() {
            return averageDuplicateRate;
        }
        
        public void setAverageDuplicateRate(Double averageDuplicateRate) {
            this.averageDuplicateRate = averageDuplicateRate;
        }
        
        public Double getAverageMaintainabilityIndex() {
            return averageMaintainabilityIndex;
        }
        
        public void setAverageMaintainabilityIndex(Double averageMaintainabilityIndex) {
            this.averageMaintainabilityIndex = averageMaintainabilityIndex;
        }
        
        public Double getAverageTechnicalDebt() {
            return averageTechnicalDebt;
        }
        
        public void setAverageTechnicalDebt(Double averageTechnicalDebt) {
            this.averageTechnicalDebt = averageTechnicalDebt;
        }
        
        public List<QualityTrendPoint> getTrendPoints() {
            return trendPoints;
        }
        
        public void setTrendPoints(List<QualityTrendPoint> trendPoints) {
            this.trendPoints = trendPoints;
        }
    }
    
    /**
     * 质量趋势点数据
     */
    public static class QualityTrendPoint {
        private LocalDateTime date;
        private Double complexity;
        private Double duplicateRate;
        private Double maintainabilityIndex;
        private Double technicalDebt;
        
        // Getters and Setters
        public LocalDateTime getDate() {
            return date;
        }
        
        public void setDate(LocalDateTime date) {
            this.date = date;
        }
        
        public Double getComplexity() {
            return complexity;
        }
        
        public void setComplexity(Double complexity) {
            this.complexity = complexity;
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
        
        public Double getTechnicalDebt() {
            return technicalDebt;
        }
        
        public void setTechnicalDebt(Double technicalDebt) {
            this.technicalDebt = technicalDebt;
        }
    }
    
    /**
     * 质量概览数据
     */
    public static class QualityOverview {
        private Integer totalScans;
        private Integer passedGates;
        private Integer failedGates;
        private Double passRate;
        private Integer totalBugs;
        private Integer totalVulnerabilities;
        private Integer totalCodeSmells;
        
        // Getters and Setters
        public Integer getTotalScans() {
            return totalScans;
        }
        
        public void setTotalScans(Integer totalScans) {
            this.totalScans = totalScans;
        }
        
        public Integer getPassedGates() {
            return passedGates;
        }
        
        public void setPassedGates(Integer passedGates) {
            this.passedGates = passedGates;
        }
        
        public Integer getFailedGates() {
            return failedGates;
        }
        
        public void setFailedGates(Integer failedGates) {
            this.failedGates = failedGates;
        }
        
        public Double getPassRate() {
            return passRate;
        }
        
        public void setPassRate(Double passRate) {
            this.passRate = passRate;
        }
        
        public Integer getTotalBugs() {
            return totalBugs;
        }
        
        public void setTotalBugs(Integer totalBugs) {
            this.totalBugs = totalBugs;
        }
        
        public Integer getTotalVulnerabilities() {
            return totalVulnerabilities;
        }
        
        public void setTotalVulnerabilities(Integer totalVulnerabilities) {
            this.totalVulnerabilities = totalVulnerabilities;
        }
        
        public Integer getTotalCodeSmells() {
            return totalCodeSmells;
        }
        
        public void setTotalCodeSmells(Integer totalCodeSmells) {
            this.totalCodeSmells = totalCodeSmells;
        }
    }
}