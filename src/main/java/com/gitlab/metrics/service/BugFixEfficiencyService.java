package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bug Fix Efficiency Service
 * Calculates bug fix efficiency metrics including response time, resolution time, and efficiency statistics
 */
@Service
public class BugFixEfficiencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(BugFixEfficiencyService.class);
    
    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private AlertService alertService;
    
    // Configuration parameters: Expected fix time for different severity bugs (hours)
    @Value("${bug.fix.timeout.critical:4}")
    private int criticalBugTimeoutHours;
    
    @Value("${bug.fix.timeout.high:24}")
    private int highBugTimeoutHours;
    
    @Value("${bug.fix.timeout.medium:72}")
    private int mediumBugTimeoutHours;
    
    @Value("${bug.fix.timeout.low:168}")
    private int lowBugTimeoutHours;
    
    /**
     * Calculate Bug fix efficiency statistics
     */
    public BugFixEfficiencyStats calculateBugFixEfficiency(String projectId, String assigneeId, 
                                                          LocalDateTime startTime, LocalDateTime endTime) {
        try {
            logger.info("Calculating Bug fix efficiency: projectId={}, assigneeId={}, start={}, end={}", 
                projectId, assigneeId, startTime, endTime);
            
            BugFixEfficiencyStats stats = new BugFixEfficiencyStats();
            stats.setProjectId(projectId);
            stats.setAssigneeId(assigneeId);
            stats.setStartTime(startTime);
            stats.setEndTime(endTime);
            stats.setCalculationTime(LocalDateTime.now());
            
            // Get Bug list
            List<Issue> bugs = getBugList(projectId, assigneeId, startTime, endTime);
            
            if (bugs.isEmpty()) {
                logger.info("No Bug data found under specified conditions");
                return stats;
            }
            
            // Calculate basic statistics
            calculateBasicStats(stats, bugs);
            
            // Statistics by severity
            calculateSeverityStats(stats, bugs);
            
            // Statistics by personnel (if not filtering by specific assignee)
            if (StringUtils.isEmpty(assigneeId)) {
                calculateDeveloperStats(stats, bugs);
            }
            
            logger.info("Bug fix efficiency calculation completed: Total Bugs={}, Average fix time={}hours", 
                stats.getTotalBugs(), stats.getAverageResolutionTimeHours());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to calculate Bug fix efficiency: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate Bug fix efficiency", e);
        }
    }
    
    /**
     * Get Bug list based on filters
     */
    private List<Issue> getBugList(String projectId, String assigneeId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Issue> allIssues;
        
        if (StringUtils.hasText(projectId) && StringUtils.hasText(assigneeId)) {
            // Query by project and assignee
            allIssues = issueRepository.findByAssigneeIdAndCreatedAtBetween(assigneeId, startTime, endTime)
                .stream()
                .filter(issue -> projectId.equals(issue.getProjectId()))
                .collect(Collectors.toList());
        } else if (StringUtils.hasText(projectId)) {
            // Query by project
            allIssues = issueRepository.findByProjectIdAndCreatedAtBetween(projectId, startTime, endTime);
        } else if (StringUtils.hasText(assigneeId)) {
            // Query by assignee
            allIssues = issueRepository.findByAssigneeIdAndCreatedAtBetween(assigneeId, startTime, endTime);
        } else {
            // Query all within time range
            allIssues = issueRepository.findAll().stream()
                .filter(issue -> issue.getCreatedAt().isAfter(startTime) && issue.getCreatedAt().isBefore(endTime))
                .collect(Collectors.toList());
        }
        
        // Only return Bug type Issues
        return allIssues.stream()
            .filter(issue -> "bug".equals(issue.getIssueType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate basic statistics
     */
    private void calculateBasicStats(BugFixEfficiencyStats stats, List<Issue> bugs) {
        stats.setTotalBugs(bugs.size());
        
        List<Issue> closedBugs = bugs.stream()
            .filter(bug -> "closed".equals(bug.getStatus()))
            .collect(Collectors.toList());
        
        stats.setClosedBugs(closedBugs.size());
        stats.setOpenBugs(stats.getTotalBugs() - stats.getClosedBugs());
        
        if (!closedBugs.isEmpty()) {
            // Calculate fix time statistics
            List<Long> resolutionTimes = closedBugs.stream()
                .filter(bug -> bug.getResolutionTimeMinutes() != null)
                .map(Issue::getResolutionTimeMinutes)
                .collect(Collectors.toList());
            
            if (!resolutionTimes.isEmpty()) {
                stats.setAverageResolutionTimeHours(resolutionTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .average().orElse(0.0));
                
                stats.setMinResolutionTimeHours(resolutionTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .min().orElse(0.0));
                
                stats.setMaxResolutionTimeHours(resolutionTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .max().orElse(0.0));
                
                stats.setMedianResolutionTimeHours(calculateMedian(resolutionTimes) / 60.0);
            }
            
            // Calculate response time statistics
            List<Long> responseTimes = closedBugs.stream()
                .filter(bug -> bug.getResponseTimeMinutes() != null)
                .map(Issue::getResponseTimeMinutes)
                .collect(Collectors.toList());
            
            if (!responseTimes.isEmpty()) {
                stats.setAverageResponseTimeHours(responseTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .average().orElse(0.0));
                
                stats.setMinResponseTimeHours(responseTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .min().orElse(0.0));
                
                stats.setMaxResponseTimeHours(responseTimes.stream()
                    .mapToDouble(time -> time / 60.0)
                    .max().orElse(0.0));
                
                stats.setMedianResponseTimeHours(calculateMedian(responseTimes) / 60.0);
            }
        }
        
        // Calculate resolution rate
        if (stats.getTotalBugs() > 0) {
            stats.setResolutionRate((double) stats.getClosedBugs() / stats.getTotalBugs() * 100);
        }
    }
    
    /**
     * Statistics by severity
     */
    private void calculateSeverityStats(BugFixEfficiencyStats stats, List<Issue> bugs) {
        Map<String, List<Issue>> severityGroups = bugs.stream()
            .filter(bug -> StringUtils.hasText(bug.getSeverity()))
            .collect(Collectors.groupingBy(Issue::getSeverity));
        
        for (Map.Entry<String, List<Issue>> entry : severityGroups.entrySet()) {
            String severity = entry.getKey();
            List<Issue> severityBugs = entry.getValue();
            
            BugFixEfficiencyStats.SeverityStats severityStats = new BugFixEfficiencyStats.SeverityStats();
            severityStats.setSeverity(severity);
            severityStats.setCount(severityBugs.size());
            
            List<Issue> closedSeverityBugs = severityBugs.stream()
                .filter(bug -> "closed".equals(bug.getStatus()))
                .collect(Collectors.toList());
            
            severityStats.setClosedCount(closedSeverityBugs.size());
            
            if (!closedSeverityBugs.isEmpty()) {
                // Fix time statistics
                List<Long> resolutionTimes = closedSeverityBugs.stream()
                    .filter(bug -> bug.getResolutionTimeMinutes() != null)
                    .map(Issue::getResolutionTimeMinutes)
                    .collect(Collectors.toList());
                
                if (!resolutionTimes.isEmpty()) {
                    severityStats.setAverageResolutionTimeHours(resolutionTimes.stream()
                        .mapToDouble(time -> time / 60.0)
                        .average().orElse(0.0));
                }
                
                // Response time statistics
                List<Long> responseTimes = closedSeverityBugs.stream()
                    .filter(bug -> bug.getResponseTimeMinutes() != null)
                    .map(Issue::getResponseTimeMinutes)
                    .collect(Collectors.toList());
                
                if (!responseTimes.isEmpty()) {
                    severityStats.setAverageResponseTimeHours(responseTimes.stream()
                        .mapToDouble(time -> time / 60.0)
                        .average().orElse(0.0));
                }
            }
            
            // Calculate resolution rate
            if (severityStats.getCount() > 0) {
                severityStats.setResolutionRate((double) severityStats.getClosedCount() / severityStats.getCount() * 100);
            }
            
            // Check timeout count
            int timeoutHours = getTimeoutHoursBySeverity(severity);
            severityStats.setTimeoutCount((int) severityBugs.stream()
                .filter(bug -> "opened".equals(bug.getStatus()))
                .filter(bug -> ChronoUnit.HOURS.between(bug.getCreatedAt(), LocalDateTime.now()) > timeoutHours)
                .count());
            
            stats.getSeverityStats().add(severityStats);
        }
        
        // Sort by average fix time
        stats.getSeverityStats().sort(Comparator.comparing(BugFixEfficiencyStats.SeverityStats::getAverageResolutionTimeHours));
    }
    
    /**
     * Statistics by personnel
     */
    private void calculateDeveloperStats(BugFixEfficiencyStats stats, List<Issue> bugs) {
        Map<String, List<Issue>> developerGroups = bugs.stream()
            .filter(bug -> StringUtils.hasText(bug.getAssigneeId()))
            .collect(Collectors.groupingBy(Issue::getAssigneeId));
        
        for (Map.Entry<String, List<Issue>> entry : developerGroups.entrySet()) {
            String developerId = entry.getKey();
            List<Issue> developerBugs = entry.getValue();
            
            BugFixEfficiencyStats.DeveloperStats developerStats = new BugFixEfficiencyStats.DeveloperStats();
            developerStats.setDeveloperId(developerId);
            developerStats.setDeveloperName(developerBugs.get(0).getAssigneeName());
            developerStats.setCount(developerBugs.size());
            
            List<Issue> closedDeveloperBugs = developerBugs.stream()
                .filter(bug -> "closed".equals(bug.getStatus()))
                .collect(Collectors.toList());
            
            developerStats.setClosedCount(closedDeveloperBugs.size());
            
            if (!closedDeveloperBugs.isEmpty()) {
                // Fix time statistics
                List<Long> resolutionTimes = closedDeveloperBugs.stream()
                    .filter(bug -> bug.getResolutionTimeMinutes() != null)
                    .map(Issue::getResolutionTimeMinutes)
                    .collect(Collectors.toList());
                
                if (!resolutionTimes.isEmpty()) {
                    developerStats.setAverageResolutionTimeHours(resolutionTimes.stream()
                        .mapToDouble(time -> time / 60.0)
                        .average().orElse(0.0));
                }
                
                // Response time statistics
                List<Long> responseTimes = closedDeveloperBugs.stream()
                    .filter(bug -> bug.getResponseTimeMinutes() != null)
                    .map(Issue::getResponseTimeMinutes)
                    .collect(Collectors.toList());
                
                if (!responseTimes.isEmpty()) {
                    developerStats.setAverageResponseTimeHours(responseTimes.stream()
                        .mapToDouble(time -> time / 60.0)
                        .average().orElse(0.0));
                }
            }
            
            // Calculate resolution rate
            if (developerStats.getCount() > 0) {
                developerStats.setResolutionRate((double) developerStats.getClosedCount() / developerStats.getCount() * 100);
            }
            
            // Calculate efficiency score (based on fix time and resolution rate)
            double efficiencyScore = calculateDeveloperEfficiencyScore(developerStats);
            developerStats.setEfficiencyScore(efficiencyScore);
            
            stats.getDeveloperStats().add(developerStats);
        }
        
        // Sort by efficiency score (high score first)
        stats.getDeveloperStats().sort(Comparator.comparing(BugFixEfficiencyStats.DeveloperStats::getEfficiencyScore).reversed());
    }
    
    /**
     * Calculate median value
     */
    private double calculateMedian(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    /**
     * Get timeout hours by severity
     */
    private int getTimeoutHoursBySeverity(String severity) {
        switch (severity.toLowerCase()) {
            case "critical":
            case "blocker":
                return criticalBugTimeoutHours;
            case "high":
                return highBugTimeoutHours;
            case "medium":
                return mediumBugTimeoutHours;
            case "low":
            default:
                return lowBugTimeoutHours;
        }
    }
    
    /**
     * Calculate developer efficiency score
     */
    private double calculateDeveloperEfficiencyScore(BugFixEfficiencyStats.DeveloperStats stats) {
        // Base score from resolution rate (0-100)
        double resolutionScore = stats.getResolutionRate();
        
        // Time efficiency score (inverse of average resolution time, normalized)
        double timeScore = 0.0;
        if (stats.getAverageResolutionTimeHours() > 0) {
            // Lower time = higher score, max score 100 for <= 4 hours
            timeScore = Math.max(0, 100 - (stats.getAverageResolutionTimeHours() - 4) * 2);
        }
        
        // Combined score (weighted average: 60% resolution rate, 40% time efficiency)
        return resolutionScore * 0.6 + timeScore * 0.4;
    }
    
    /**
     * Scheduled check for timeout bugs and send alerts
     */
    @Scheduled(fixedRate = 3600000) // Execute once per hour
    public void checkTimeoutBugsAndSendAlerts() {
        try {
            logger.info("Starting timeout bug check");
            
            // Check timeout bugs of various severity levels
            checkTimeoutBugsBySeverity("critical", criticalBugTimeoutHours);
            checkTimeoutBugsBySeverity("high", highBugTimeoutHours);
            checkTimeoutBugsBySeverity("medium", mediumBugTimeoutHours);
            checkTimeoutBugsBySeverity("low", lowBugTimeoutHours);
            
            logger.info("Timeout bug check completed");
            
        } catch (Exception e) {
            logger.error("Failed to check timeout bugs: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check timeout bugs by severity and send alerts
     */
    private void checkTimeoutBugsBySeverity(String severity, int timeoutHours) {
        try {
            List<Issue> timeoutBugs = issueRepository.findByStatusOrderByCreatedAtDesc("opened")
                .stream()
                .filter(bug -> "bug".equals(bug.getIssueType()))
                .filter(bug -> severity.equals(bug.getSeverity()))
                .filter(bug -> ChronoUnit.HOURS.between(bug.getCreatedAt(), LocalDateTime.now()) > timeoutHours)
                .collect(Collectors.toList());
            
            for (Issue bug : timeoutBugs) {
                long hoursOverdue = ChronoUnit.HOURS.between(bug.getCreatedAt(), LocalDateTime.now()) - timeoutHours;
                sendTimeoutAlert(bug, hoursOverdue);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check timeout bugs for severity {}: {}", severity, e.getMessage(), e);
        }
    }
    
    /**
     * Send timeout alert
     */
    private void sendTimeoutAlert(Issue bug, long hoursOverdue) {
        try {
            String message = String.format(
                "Bug timeout alert: Bug #%s (%s severity) has been open for %d hours overdue. " +
                "Assigned to: %s, Project: %s",
                bug.getIssueId(), bug.getSeverity(), hoursOverdue, 
                bug.getAssigneeName(), bug.getProjectId()
            );
            
            AlertService.Alert alert = new AlertService.Alert();
            alert.setType(AlertService.AlertType.BUG_TIMEOUT);
            alert.setLevel(AlertService.AlertLevel.HIGH);
            alert.setMessage(message);
            alert.setProjectId(bug.getProjectId());
            alert.setAssigneeId(bug.getAssigneeId());
            alert.setIssueId(bug.getIssueId());
            alert.setCreatedAt(LocalDateTime.now());
            
            alertService.sendAlert(alert);
            
            logger.info("Sent timeout alert for bug: {}", bug.getIssueId());
            
        } catch (Exception e) {
            logger.error("Failed to send timeout alert for bug {}: {}", bug.getIssueId(), e.getMessage(), e);
        }
    }
    
    /**
     * 比较两个时间段的Bug修复效率
     */
    public BugFixEfficiencyComparison compareBugFixEfficiency(String projectId, 
                                                             LocalDateTime period1Start, LocalDateTime period1End,
                                                             LocalDateTime period2Start, LocalDateTime period2End) {
        try {
            logger.info("Comparing bug fix efficiency for project: {}", projectId);
            
            // 获取两个时间段的Bug修复统计
            BugFixEfficiencyStats period1Stats = calculateBugFixEfficiency(projectId, null, period1Start, period1End);
            BugFixEfficiencyStats period2Stats = calculateBugFixEfficiency(projectId, null, period2Start, period2End);
            
            BugFixEfficiencyComparison comparison = new BugFixEfficiencyComparison();
            comparison.setProjectId(projectId);
            comparison.setPeriod1Start(period1Start);
            comparison.setPeriod1End(period1End);
            comparison.setPeriod2Start(period2Start);
            comparison.setPeriod2End(period2End);
            comparison.setPeriod1Stats(period1Stats);
            comparison.setPeriod2Stats(period2Stats);
            
            // 计算改进指标
            if (period2Stats.getAverageResolutionTimeHours() > 0 && period1Stats.getAverageResolutionTimeHours() > 0) {
                double fixTimeImprovement = ((period1Stats.getAverageResolutionTimeHours() - period2Stats.getAverageResolutionTimeHours()) 
                                           / period1Stats.getAverageResolutionTimeHours()) * 100;
                comparison.setFixTimeImprovement(fixTimeImprovement);
            }
            
            if (period2Stats.getAverageResponseTimeHours() > 0 && period1Stats.getAverageResponseTimeHours() > 0) {
                double responseTimeImprovement = ((period1Stats.getAverageResponseTimeHours() - period2Stats.getAverageResponseTimeHours()) 
                                                / period1Stats.getAverageResponseTimeHours()) * 100;
                comparison.setResponseTimeImprovement(responseTimeImprovement);
            }
            
            return comparison;
            
        } catch (Exception e) {
            logger.error("Failed to compare bug fix efficiency: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to compare bug fix efficiency: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取长时间未解决的Bug
     */
    public List<Issue> getLongPendingIssues(String projectId, Integer hoursThreshold) {
        try {
            logger.info("Getting long pending issues: projectId={}, threshold={}h", projectId, hoursThreshold);
            
            if (projectId != null) {
                return issueRepository.findLongPendingIssuesByProject(projectId, hoursThreshold);
            } else {
                return issueRepository.findLongPendingIssues(hoursThreshold);
            }
            
        } catch (Exception e) {
            logger.error("Failed to get long pending issues: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get long pending issues: " + e.getMessage(), e);
        }
    }
    
    /**
     * Bug修复效率比较结果类
     */
    public static class BugFixEfficiencyComparison {
        private String projectId;
        private LocalDateTime period1Start;
        private LocalDateTime period1End;
        private LocalDateTime period2Start;
        private LocalDateTime period2End;
        private BugFixEfficiencyStats period1Stats;
        private BugFixEfficiencyStats period2Stats;
        private Double fixTimeImprovement; // 修复时间改进百分比
        private Double responseTimeImprovement; // 响应时间改进百分比
        private LocalDateTime comparisonTime; // 比较时间
        private Double resolutionRateImprovement; // 解决率改进
        private Integer bugCountChange; // Bug数量变化
        private String overallAssessment; // 整体评估
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getPeriod1Start() { return period1Start; }
        public void setPeriod1Start(LocalDateTime period1Start) { this.period1Start = period1Start; }
        
        public LocalDateTime getPeriod1End() { return period1End; }
        public void setPeriod1End(LocalDateTime period1End) { this.period1End = period1End; }
        
        public LocalDateTime getPeriod2Start() { return period2Start; }
        public void setPeriod2Start(LocalDateTime period2Start) { this.period2Start = period2Start; }
        
        public LocalDateTime getPeriod2End() { return period2End; }
        public void setPeriod2End(LocalDateTime period2End) { this.period2End = period2End; }
        
        public BugFixEfficiencyStats getPeriod1Stats() { return period1Stats; }
        public void setPeriod1Stats(BugFixEfficiencyStats period1Stats) { this.period1Stats = period1Stats; }
        
        public BugFixEfficiencyStats getPeriod2Stats() { return period2Stats; }
        public void setPeriod2Stats(BugFixEfficiencyStats period2Stats) { this.period2Stats = period2Stats; }
        
        public Double getFixTimeImprovement() { return fixTimeImprovement; }
        public void setFixTimeImprovement(Double fixTimeImprovement) { this.fixTimeImprovement = fixTimeImprovement; }
        
        public Double getResponseTimeImprovement() { return responseTimeImprovement; }
        public void setResponseTimeImprovement(Double responseTimeImprovement) { this.responseTimeImprovement = responseTimeImprovement; }
        
        public LocalDateTime getComparisonTime() { return comparisonTime; }
        public void setComparisonTime(LocalDateTime comparisonTime) { this.comparisonTime = comparisonTime; }
        
        public Double getResolutionRateImprovement() { return resolutionRateImprovement; }
        public void setResolutionRateImprovement(Double resolutionRateImprovement) { this.resolutionRateImprovement = resolutionRateImprovement; }
        
        public Integer getBugCountChange() { return bugCountChange; }
        public void setBugCountChange(Integer bugCountChange) { this.bugCountChange = bugCountChange; }
        
        public String getOverallAssessment() { return overallAssessment; }
        public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }
    }
}