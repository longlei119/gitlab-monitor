package com.gitlab.metrics.dto;

import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.service.TestCoverageService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试覆盖率响应DTO
 * 用于返回测试覆盖率API的响应数据
 */
public class TestCoverageResponse {
    
    private String projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timeRange;
    private List<TestCoverage> coverageRecords;
    private TestCoverageService.CoverageTrend trendData;
    private CoverageOverview overview;
    
    public TestCoverageResponse() {}
    
    public TestCoverageResponse(String projectId, String timeRange) {
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
    
    public List<TestCoverage> getCoverageRecords() {
        return coverageRecords;
    }
    
    public void setCoverageRecords(List<TestCoverage> coverageRecords) {
        this.coverageRecords = coverageRecords;
    }
    
    public TestCoverageService.CoverageTrend getTrendData() {
        return trendData;
    }
    
    public void setTrendData(TestCoverageService.CoverageTrend trendData) {
        this.trendData = trendData;
    }
    
    public CoverageOverview getOverview() {
        return overview;
    }
    
    public void setOverview(CoverageOverview overview) {
        this.overview = overview;
    }
    
    /**
     * 覆盖率概览数据
     */
    public static class CoverageOverview {
        private Integer totalReports;
        private Integer passedReports;
        private Integer failedReports;
        private Double passRate;
        private Double averageLineCoverage;
        private Double averageBranchCoverage;
        private Double averageFunctionCoverage;
        
        // Getters and Setters
        public Integer getTotalReports() {
            return totalReports;
        }
        
        public void setTotalReports(Integer totalReports) {
            this.totalReports = totalReports;
        }
        
        public Integer getPassedReports() {
            return passedReports;
        }
        
        public void setPassedReports(Integer passedReports) {
            this.passedReports = passedReports;
        }
        
        public Integer getFailedReports() {
            return failedReports;
        }
        
        public void setFailedReports(Integer failedReports) {
            this.failedReports = failedReports;
        }
        
        public Double getPassRate() {
            return passRate;
        }
        
        public void setPassRate(Double passRate) {
            this.passRate = passRate;
        }
        
        public Double getAverageLineCoverage() {
            return averageLineCoverage;
        }
        
        public void setAverageLineCoverage(Double averageLineCoverage) {
            this.averageLineCoverage = averageLineCoverage;
        }
        
        public Double getAverageBranchCoverage() {
            return averageBranchCoverage;
        }
        
        public void setAverageBranchCoverage(Double averageBranchCoverage) {
            this.averageBranchCoverage = averageBranchCoverage;
        }
        
        public Double getAverageFunctionCoverage() {
            return averageFunctionCoverage;
        }
        
        public void setAverageFunctionCoverage(Double averageFunctionCoverage) {
            this.averageFunctionCoverage = averageFunctionCoverage;
        }
    }
}