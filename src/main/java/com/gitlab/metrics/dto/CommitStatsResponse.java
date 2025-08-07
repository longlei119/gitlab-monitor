package com.gitlab.metrics.dto;

import com.gitlab.metrics.service.CommitStatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码提交统计响应DTO
 * 用于返回提交统计API的响应数据
 */
public class CommitStatsResponse {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String projectId;
    private String developerId;
    private List<CommitStatisticsService.DeveloperCommitStats> developerStats;
    private List<CommitStatisticsService.ProjectCommitStats> projectStats;
    private List<CommitStatisticsService.CommitTrendData> trendData;
    private CommitStatisticsService.ProjectTotalStats totalStats;
    
    public CommitStatsResponse() {}
    
    public CommitStatsResponse(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
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
    
    public List<CommitStatisticsService.DeveloperCommitStats> getDeveloperStats() {
        return developerStats;
    }
    
    public void setDeveloperStats(List<CommitStatisticsService.DeveloperCommitStats> developerStats) {
        this.developerStats = developerStats;
    }
    
    public List<CommitStatisticsService.ProjectCommitStats> getProjectStats() {
        return projectStats;
    }
    
    public void setProjectStats(List<CommitStatisticsService.ProjectCommitStats> projectStats) {
        this.projectStats = projectStats;
    }
    
    public List<CommitStatisticsService.CommitTrendData> getTrendData() {
        return trendData;
    }
    
    public void setTrendData(List<CommitStatisticsService.CommitTrendData> trendData) {
        this.trendData = trendData;
    }
    
    public CommitStatisticsService.ProjectTotalStats getTotalStats() {
        return totalStats;
    }
    
    public void setTotalStats(CommitStatisticsService.ProjectTotalStats totalStats) {
        this.totalStats = totalStats;
    }
}