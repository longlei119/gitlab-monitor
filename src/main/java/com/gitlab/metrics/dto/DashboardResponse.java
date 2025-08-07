package com.gitlab.metrics.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 效率看板响应DTO
 * 用于返回综合的开发效率看板数据
 */
public class DashboardResponse {
    
    private String projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timeRange;
    private OverallMetrics overallMetrics;
    private DeveloperEfficiency developerEfficiency;
    private QualityTrends qualityTrends;
    private ProductivityMetrics productivityMetrics;
    private List<TrendPoint> trendData;
    
    public DashboardResponse() {}
    
    public DashboardResponse(String projectId, String timeRange) {
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
    
    public OverallMetrics getOverallMetrics() {
        return overallMetrics;
    }
    
    public void setOverallMetrics(OverallMetrics overallMetrics) {
        this.overallMetrics = overallMetrics;
    }
    
    public DeveloperEfficiency getDeveloperEfficiency() {
        return developerEfficiency;
    }
    
    public void setDeveloperEfficiency(DeveloperEfficiency developerEfficiency) {
        this.developerEfficiency = developerEfficiency;
    }
    
    public QualityTrends getQualityTrends() {
        return qualityTrends;
    }
    
    public void setQualityTrends(QualityTrends qualityTrends) {
        this.qualityTrends = qualityTrends;
    }
    
    public ProductivityMetrics getProductivityMetrics() {
        return productivityMetrics;
    }
    
    public void setProductivityMetrics(ProductivityMetrics productivityMetrics) {
        this.productivityMetrics = productivityMetrics;
    }
    
    public List<TrendPoint> getTrendData() {
        return trendData;
    }
    
    public void setTrendData(List<TrendPoint> trendData) {
        this.trendData = trendData;
    }
    
    /**
     * 总体指标
     */
    public static class OverallMetrics {
        private Integer totalCommits;
        private Integer totalDevelopers;
        private Integer totalLinesAdded;
        private Integer totalLinesDeleted;
        private Integer totalMergeRequests;
        private Integer totalIssues;
        private Double averageCommitSize;
        private Double codeChurnRate;
        
        // Getters and Setters
        public Integer getTotalCommits() {
            return totalCommits;
        }
        
        public void setTotalCommits(Integer totalCommits) {
            this.totalCommits = totalCommits;
        }
        
        public Integer getTotalDevelopers() {
            return totalDevelopers;
        }
        
        public void setTotalDevelopers(Integer totalDevelopers) {
            this.totalDevelopers = totalDevelopers;
        }
        
        public Integer getTotalLinesAdded() {
            return totalLinesAdded;
        }
        
        public void setTotalLinesAdded(Integer totalLinesAdded) {
            this.totalLinesAdded = totalLinesAdded;
        }
        
        public Integer getTotalLinesDeleted() {
            return totalLinesDeleted;
        }
        
        public void setTotalLinesDeleted(Integer totalLinesDeleted) {
            this.totalLinesDeleted = totalLinesDeleted;
        }
        
        public Integer getTotalMergeRequests() {
            return totalMergeRequests;
        }
        
        public void setTotalMergeRequests(Integer totalMergeRequests) {
            this.totalMergeRequests = totalMergeRequests;
        }
        
        public Integer getTotalIssues() {
            return totalIssues;
        }
        
        public void setTotalIssues(Integer totalIssues) {
            this.totalIssues = totalIssues;
        }
        
        public Double getAverageCommitSize() {
            return averageCommitSize;
        }
        
        public void setAverageCommitSize(Double averageCommitSize) {
            this.averageCommitSize = averageCommitSize;
        }
        
        public Double getCodeChurnRate() {
            return codeChurnRate;
        }
        
        public void setCodeChurnRate(Double codeChurnRate) {
            this.codeChurnRate = codeChurnRate;
        }
    }
    
    /**
     * 开发者效率
     */
    public static class DeveloperEfficiency {
        private List<DeveloperMetric> topDevelopers;
        private Double averageCommitsPerDeveloper;
        private Double averageLinesPerDeveloper;
        private Map<String, Integer> developerDistribution;
        
        // Getters and Setters
        public List<DeveloperMetric> getTopDevelopers() {
            return topDevelopers;
        }
        
        public void setTopDevelopers(List<DeveloperMetric> topDevelopers) {
            this.topDevelopers = topDevelopers;
        }
        
        public Double getAverageCommitsPerDeveloper() {
            return averageCommitsPerDeveloper;
        }
        
        public void setAverageCommitsPerDeveloper(Double averageCommitsPerDeveloper) {
            this.averageCommitsPerDeveloper = averageCommitsPerDeveloper;
        }
        
        public Double getAverageLinesPerDeveloper() {
            return averageLinesPerDeveloper;
        }
        
        public void setAverageLinesPerDeveloper(Double averageLinesPerDeveloper) {
            this.averageLinesPerDeveloper = averageLinesPerDeveloper;
        }
        
        public Map<String, Integer> getDeveloperDistribution() {
            return developerDistribution;
        }
        
        public void setDeveloperDistribution(Map<String, Integer> developerDistribution) {
            this.developerDistribution = developerDistribution;
        }
    }
    
    /**
     * 开发者指标
     */
    public static class DeveloperMetric {
        private String developerId;
        private String developerName;
        private Integer commits;
        private Integer linesAdded;
        private Integer linesDeleted;
        private Integer filesChanged;
        private Double efficiency;
        
        // Getters and Setters
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
        
        public Integer getCommits() {
            return commits;
        }
        
        public void setCommits(Integer commits) {
            this.commits = commits;
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
        
        public Double getEfficiency() {
            return efficiency;
        }
        
        public void setEfficiency(Double efficiency) {
            this.efficiency = efficiency;
        }
    }
    
    /**
     * 质量趋势
     */
    public static class QualityTrends {
        private Double currentQualityScore;
        private Double previousQualityScore;
        private Double qualityChange;
        private Integer totalBugs;
        private Integer fixedBugs;
        private Double bugFixRate;
        private Double averageFixTime;
        private Double testCoverage;
        private Double testCoverageChange;
        
        // Getters and Setters
        public Double getCurrentQualityScore() {
            return currentQualityScore;
        }
        
        public void setCurrentQualityScore(Double currentQualityScore) {
            this.currentQualityScore = currentQualityScore;
        }
        
        public Double getPreviousQualityScore() {
            return previousQualityScore;
        }
        
        public void setPreviousQualityScore(Double previousQualityScore) {
            this.previousQualityScore = previousQualityScore;
        }
        
        public Double getQualityChange() {
            return qualityChange;
        }
        
        public void setQualityChange(Double qualityChange) {
            this.qualityChange = qualityChange;
        }
        
        public Integer getTotalBugs() {
            return totalBugs;
        }
        
        public void setTotalBugs(Integer totalBugs) {
            this.totalBugs = totalBugs;
        }
        
        public Integer getFixedBugs() {
            return fixedBugs;
        }
        
        public void setFixedBugs(Integer fixedBugs) {
            this.fixedBugs = fixedBugs;
        }
        
        public Double getBugFixRate() {
            return bugFixRate;
        }
        
        public void setBugFixRate(Double bugFixRate) {
            this.bugFixRate = bugFixRate;
        }
        
        public Double getAverageFixTime() {
            return averageFixTime;
        }
        
        public void setAverageFixTime(Double averageFixTime) {
            this.averageFixTime = averageFixTime;
        }
        
        public Double getTestCoverage() {
            return testCoverage;
        }
        
        public void setTestCoverage(Double testCoverage) {
            this.testCoverage = testCoverage;
        }
        
        public Double getTestCoverageChange() {
            return testCoverageChange;
        }
        
        public void setTestCoverageChange(Double testCoverageChange) {
            this.testCoverageChange = testCoverageChange;
        }
    }
    
    /**
     * 生产力指标
     */
    public static class ProductivityMetrics {
        private Double commitsPerDay;
        private Double linesPerDay;
        private Double mergeRequestsPerDay;
        private Double averageMergeTime;
        private Double codeReviewEfficiency;
        private Integer activeContributors;
        
        // Getters and Setters
        public Double getCommitsPerDay() {
            return commitsPerDay;
        }
        
        public void setCommitsPerDay(Double commitsPerDay) {
            this.commitsPerDay = commitsPerDay;
        }
        
        public Double getLinesPerDay() {
            return linesPerDay;
        }
        
        public void setLinesPerDay(Double linesPerDay) {
            this.linesPerDay = linesPerDay;
        }
        
        public Double getMergeRequestsPerDay() {
            return mergeRequestsPerDay;
        }
        
        public void setMergeRequestsPerDay(Double mergeRequestsPerDay) {
            this.mergeRequestsPerDay = mergeRequestsPerDay;
        }
        
        public Double getAverageMergeTime() {
            return averageMergeTime;
        }
        
        public void setAverageMergeTime(Double averageMergeTime) {
            this.averageMergeTime = averageMergeTime;
        }
        
        public Double getCodeReviewEfficiency() {
            return codeReviewEfficiency;
        }
        
        public void setCodeReviewEfficiency(Double codeReviewEfficiency) {
            this.codeReviewEfficiency = codeReviewEfficiency;
        }
        
        public Integer getActiveContributors() {
            return activeContributors;
        }
        
        public void setActiveContributors(Integer activeContributors) {
            this.activeContributors = activeContributors;
        }
    }
    
    /**
     * 趋势点数据
     */
    public static class TrendPoint {
        private LocalDateTime date;
        private Integer commits;
        private Integer linesAdded;
        private Integer linesDeleted;
        private Double qualityScore;
        private Double testCoverage;
        private Integer bugs;
        
        // Getters and Setters
        public LocalDateTime getDate() {
            return date;
        }
        
        public void setDate(LocalDateTime date) {
            this.date = date;
        }
        
        public Integer getCommits() {
            return commits;
        }
        
        public void setCommits(Integer commits) {
            this.commits = commits;
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
        
        public Double getQualityScore() {
            return qualityScore;
        }
        
        public void setQualityScore(Double qualityScore) {
            this.qualityScore = qualityScore;
        }
        
        public Double getTestCoverage() {
            return testCoverage;
        }
        
        public void setTestCoverage(Double testCoverage) {
            this.testCoverage = testCoverage;
        }
        
        public Integer getBugs() {
            return bugs;
        }
        
        public void setBugs(Integer bugs) {
            this.bugs = bugs;
        }
    }
}