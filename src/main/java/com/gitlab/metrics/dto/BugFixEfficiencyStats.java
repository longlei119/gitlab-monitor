package com.gitlab.metrics.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bug Fix Efficiency Statistics Data Class
 */
public class BugFixEfficiencyStats {
    private String projectId;
    private String assigneeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime calculationTime;
    
    // Basic statistics
    private int totalBugs;
    private int closedBugs;
    private int openBugs;
    private double resolutionRate; // Resolution rate percentage
    
    // Fix time statistics (hours)
    private double averageResolutionTimeHours;
    private double minResolutionTimeHours;
    private double maxResolutionTimeHours;
    private double medianResolutionTimeHours;
    
    // Response time statistics (hours)
    private double averageResponseTimeHours;
    private double minResponseTimeHours;
    private double maxResponseTimeHours;
    private double medianResponseTimeHours;
    
    // Category statistics
    private List<SeverityStats> severityStats = new ArrayList<>();
    private List<PriorityStats> priorityStats = new ArrayList<>();
    private List<TypeStats> typeStats = new ArrayList<>();
    private List<DeveloperStats> developerStats = new ArrayList<>();
    private List<ProjectStats> projectStats = new ArrayList<>();
    
    // Trend data
    private List<TrendData> trendData = new ArrayList<>();
    
    // Efficiency issue identification
    private List<String> efficiencyIssues = new ArrayList<>();
    
    // Getters and Setters
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public LocalDateTime getCalculationTime() { return calculationTime; }
    public void setCalculationTime(LocalDateTime calculationTime) { this.calculationTime = calculationTime; }
    
    public int getTotalBugs() { return totalBugs; }
    public void setTotalBugs(int totalBugs) { this.totalBugs = totalBugs; }
    
    public int getClosedBugs() { return closedBugs; }
    public void setClosedBugs(int closedBugs) { this.closedBugs = closedBugs; }
    
    public int getOpenBugs() { return openBugs; }
    public void setOpenBugs(int openBugs) { this.openBugs = openBugs; }
    
    public double getResolutionRate() { return resolutionRate; }
    public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
    
    public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
    public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
    
    public double getMinResolutionTimeHours() { return minResolutionTimeHours; }
    public void setMinResolutionTimeHours(double minResolutionTimeHours) { this.minResolutionTimeHours = minResolutionTimeHours; }
    
    public double getMaxResolutionTimeHours() { return maxResolutionTimeHours; }
    public void setMaxResolutionTimeHours(double maxResolutionTimeHours) { this.maxResolutionTimeHours = maxResolutionTimeHours; }
    
    public double getMedianResolutionTimeHours() { return medianResolutionTimeHours; }
    public void setMedianResolutionTimeHours(double medianResolutionTimeHours) { this.medianResolutionTimeHours = medianResolutionTimeHours; }
    
    public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
    public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
    
    public double getMinResponseTimeHours() { return minResponseTimeHours; }
    public void setMinResponseTimeHours(double minResponseTimeHours) { this.minResponseTimeHours = minResponseTimeHours; }
    
    public double getMaxResponseTimeHours() { return maxResponseTimeHours; }
    public void setMaxResponseTimeHours(double maxResponseTimeHours) { this.maxResponseTimeHours = maxResponseTimeHours; }
    
    public double getMedianResponseTimeHours() { return medianResponseTimeHours; }
    public void setMedianResponseTimeHours(double medianResponseTimeHours) { this.medianResponseTimeHours = medianResponseTimeHours; }
    
    public List<SeverityStats> getSeverityStats() { return severityStats; }
    public void setSeverityStats(List<SeverityStats> severityStats) { this.severityStats = severityStats; }
    
    public List<PriorityStats> getPriorityStats() { return priorityStats; }
    public void setPriorityStats(List<PriorityStats> priorityStats) { this.priorityStats = priorityStats; }
    
    public List<TypeStats> getTypeStats() { return typeStats; }
    public void setTypeStats(List<TypeStats> typeStats) { this.typeStats = typeStats; }
    
    public List<DeveloperStats> getDeveloperStats() { return developerStats; }
    public void setDeveloperStats(List<DeveloperStats> developerStats) { this.developerStats = developerStats; }
    
    public List<ProjectStats> getProjectStats() { return projectStats; }
    public void setProjectStats(List<ProjectStats> projectStats) { this.projectStats = projectStats; }
    
    public List<TrendData> getTrendData() { return trendData; }
    public void setTrendData(List<TrendData> trendData) { this.trendData = trendData; }
    
    public List<String> getEfficiencyIssues() { return efficiencyIssues; }
    public void setEfficiencyIssues(List<String> efficiencyIssues) { this.efficiencyIssues = efficiencyIssues; }
    
    /**
     * Severity statistics
     */
    public static class SeverityStats {
        private String severity;
        private int count;
        private int closedCount;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        private int timeoutCount; // Number of timeout unresolved
        
        // Getters and Setters
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
        
        public int getTimeoutCount() { return timeoutCount; }
        public void setTimeoutCount(int timeoutCount) { this.timeoutCount = timeoutCount; }
    }
    
    /**
     * Priority statistics
     */
    public static class PriorityStats {
        private String priority;
        private int count;
        private int closedCount;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        
        // Getters and Setters
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
    }
    
    /**
     * Bug type statistics
     */
    public static class TypeStats {
        private String type;
        private int count;
        private int closedCount;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
    }
    
    /**
     * Developer statistics
     */
    public static class DeveloperStats {
        private String developerId;
        private String developerName;
        private int count;
        private int closedCount;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        private double efficiencyScore; // Efficiency score
        
        // Getters and Setters
        public String getDeveloperId() { return developerId; }
        public void setDeveloperId(String developerId) { this.developerId = developerId; }
        
        public String getDeveloperName() { return developerName; }
        public void setDeveloperName(String developerName) { this.developerName = developerName; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
        
        public double getEfficiencyScore() { return efficiencyScore; }
        public void setEfficiencyScore(double efficiencyScore) { this.efficiencyScore = efficiencyScore; }
    }
    
    /**
     * Project statistics
     */
    public static class ProjectStats {
        private String projectId;
        private int count;
        private int closedCount;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
    }
    
    /**
     * Trend data
     */
    public static class TrendData {
        private String date;
        private int createdCount;
        private int closedCount;
        private double averageResolutionTimeHours;
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public int getCreatedCount() { return createdCount; }
        public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
        
        public int getClosedCount() { return closedCount; }
        public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
    }
}