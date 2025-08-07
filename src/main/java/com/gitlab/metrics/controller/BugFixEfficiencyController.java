package com.gitlab.metrics.controller;

import com.gitlab.metrics.dto.BugFixEfficiencyStats;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.service.BugFixEfficiencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bug修复效率控制器
 * 提供Bug修复效率统计和分析的API接口
 */
@RestController
@RequestMapping("/api/bug-fix-efficiency")
@CrossOrigin(origins = "*")
public class BugFixEfficiencyController {
    
    private static final Logger logger = LoggerFactory.getLogger(BugFixEfficiencyController.class);
    
    @Autowired
    private BugFixEfficiencyService bugFixEfficiencyService;
    
    /**
     * 获取Bug修复效率统计
     * 
     * @param projectId 项目ID（可选）
     * @param assigneeId 分配人ID（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Bug修复效率统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<BugFixEfficiencyStats> getBugFixEfficiencyStats(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            logger.info("获取Bug修复效率统计: projectId={}, assigneeId={}, start={}, end={}", 
                projectId, assigneeId, startTime, endTime);
            
            BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
                projectId, assigneeId, startTime, endTime);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取Bug修复效率统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 比较两个时间段的Bug修复效率
     * 
     * @param projectId 项目ID
     * @param period1Start 第一个时间段开始时间
     * @param period1End 第一个时间段结束时间
     * @param period2Start 第二个时间段开始时间
     * @param period2End 第二个时间段结束时间
     * @return Bug修复效率比较结果
     */
    @GetMapping("/compare")
    public ResponseEntity<BugFixEfficiencyService.BugFixEfficiencyComparison> compareBugFixEfficiency(
            @RequestParam String projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime period1Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime period1End,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime period2Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime period2End) {
        
        try {
            logger.info("比较Bug修复效率: projectId={}, period1=[{} - {}], period2=[{} - {}]", 
                projectId, period1Start, period1End, period2Start, period2End);
            
            BugFixEfficiencyService.BugFixEfficiencyComparison comparison = 
                bugFixEfficiencyService.compareBugFixEfficiency(
                    projectId, period1Start, period1End, period2Start, period2End);
            
            return ResponseEntity.ok(comparison);
            
        } catch (Exception e) {
            logger.error("比较Bug修复效率失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取长时间未解决的Bug列表
     * 
     * @param projectId 项目ID（可选）
     * @param hoursThreshold 超时小时数阈值，默认72小时
     * @return 长时间未解决的Bug列表
     */
    @GetMapping("/long-pending")
    public ResponseEntity<List<Issue>> getLongPendingBugs(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "72") Integer hoursThreshold) {
        
        try {
            logger.info("获取长时间未解决的Bug: projectId={}, threshold={}小时", projectId, hoursThreshold);
            
            List<Issue> longPendingBugs = bugFixEfficiencyService.getLongPendingIssues(projectId, hoursThreshold);
            
            return ResponseEntity.ok(longPendingBugs);
            
        } catch (Exception e) {
            logger.error("获取长时间未解决的Bug失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取Bug修复效率概览（简化版本，用于仪表板）
     * 
     * @param projectId 项目ID（可选）
     * @param days 统计天数，默认30天
     * @return Bug修复效率概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<BugFixEfficiencyOverview> getBugFixEfficiencyOverview(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "30") Integer days) {
        
        try {
            logger.info("获取Bug修复效率概览: projectId={}, days={}", projectId, days);
            
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);
            
            BugFixEfficiencyStats stats = bugFixEfficiencyService.calculateBugFixEfficiency(
                projectId, null, startTime, endTime);
            
            // 构建概览数据
            BugFixEfficiencyOverview overview = new BugFixEfficiencyOverview();
            overview.setProjectId(projectId);
            overview.setDays(days);
            overview.setTotalBugs(stats.getTotalBugs());
            overview.setClosedBugs(stats.getClosedBugs());
            overview.setOpenBugs(stats.getOpenBugs());
            overview.setResolutionRate(stats.getResolutionRate());
            overview.setAverageResolutionTimeHours(stats.getAverageResolutionTimeHours());
            overview.setAverageResponseTimeHours(stats.getAverageResponseTimeHours());
            
            // 统计超时Bug数量
            int timeoutBugs = stats.getSeverityStats().stream()
                .mapToInt(BugFixEfficiencyStats.SeverityStats::getTimeoutCount)
                .sum();
            overview.setTimeoutBugs(timeoutBugs);
            
            // 效率问题数量
            overview.setEfficiencyIssuesCount(stats.getEfficiencyIssues().size());
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("获取Bug修复效率概览失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Bug修复效率概览数据类
     */
    public static class BugFixEfficiencyOverview {
        private String projectId;
        private Integer days;
        private int totalBugs;
        private int closedBugs;
        private int openBugs;
        private int timeoutBugs;
        private double resolutionRate;
        private double averageResolutionTimeHours;
        private double averageResponseTimeHours;
        private int efficiencyIssuesCount;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public Integer getDays() { return days; }
        public void setDays(Integer days) { this.days = days; }
        
        public int getTotalBugs() { return totalBugs; }
        public void setTotalBugs(int totalBugs) { this.totalBugs = totalBugs; }
        
        public int getClosedBugs() { return closedBugs; }
        public void setClosedBugs(int closedBugs) { this.closedBugs = closedBugs; }
        
        public int getOpenBugs() { return openBugs; }
        public void setOpenBugs(int openBugs) { this.openBugs = openBugs; }
        
        public int getTimeoutBugs() { return timeoutBugs; }
        public void setTimeoutBugs(int timeoutBugs) { this.timeoutBugs = timeoutBugs; }
        
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
        
        public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
        public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }
        
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public void setAverageResponseTimeHours(double averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }
        
        public int getEfficiencyIssuesCount() { return efficiencyIssuesCount; }
        public void setEfficiencyIssuesCount(int efficiencyIssuesCount) { this.efficiencyIssuesCount = efficiencyIssuesCount; }
    }
}