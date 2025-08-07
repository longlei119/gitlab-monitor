package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.repository.CommitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码提交统计服务
 * 提供按时间维度聚合的提交统计功能，支持按项目、分支、开发者筛选
 */
@Service
public class CommitStatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitStatisticsService.class);
    
    @Autowired
    private CommitRepository commitRepository;
    
    /**
     * 获取开发者提交统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param projectId 项目ID（可选）
     * @param developerId 开发者ID（可选）
     * @return 开发者提交统计列表
     */
    public List<DeveloperCommitStats> getDeveloperCommitStats(LocalDateTime startDate, LocalDateTime endDate, 
                                                             String projectId, String developerId) {
        logger.info("Getting developer commit stats: startDate={}, endDate={}, projectId={}, developerId={}", 
                   startDate, endDate, projectId, developerId);
        
        List<Object[]> rawStats;
        
        if (StringUtils.hasText(projectId)) {
            rawStats = commitRepository.getDeveloperCommitStatsByProject(projectId, startDate, endDate);
        } else {
            rawStats = commitRepository.getDeveloperCommitStats(startDate, endDate);
        }
        
        List<DeveloperCommitStats> stats = rawStats.stream()
            .map(this::mapToDeveloperCommitStats)
            .collect(Collectors.toList());
        
        // 如果指定了开发者ID，进行过滤
        if (StringUtils.hasText(developerId)) {
            stats = stats.stream()
                .filter(stat -> developerId.equals(stat.getDeveloperId()))
                .collect(Collectors.toList());
        }
        
        logger.info("Retrieved {} developer commit stats", stats.size());
        return stats;
    }
    
    /**
     * 获取项目提交统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 项目提交统计列表
     */
    public List<ProjectCommitStats> getProjectCommitStats(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting project commit stats: startDate={}, endDate={}", startDate, endDate);
        
        List<Object[]> rawStats = commitRepository.getProjectCommitStats(startDate, endDate);
        
        List<ProjectCommitStats> stats = rawStats.stream()
            .map(this::mapToProjectCommitStats)
            .collect(Collectors.toList());
        
        logger.info("Retrieved {} project commit stats", stats.size());
        return stats;
    }
    
    /**
     * 获取提交趋势数据（按日期）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param projectId 项目ID（可选）
     * @return 提交趋势数据列表
     */
    public List<CommitTrendData> getCommitTrend(LocalDateTime startDate, LocalDateTime endDate, String projectId) {
        logger.info("Getting commit trend: startDate={}, endDate={}, projectId={}", startDate, endDate, projectId);
        
        List<Object[]> rawTrend;
        
        if (StringUtils.hasText(projectId)) {
            rawTrend = commitRepository.getCommitTrendByProjectAndDate(projectId, startDate, endDate);
        } else {
            rawTrend = commitRepository.getCommitTrendByDate(startDate, endDate);
        }
        
        List<CommitTrendData> trendData = rawTrend.stream()
            .map(this::mapToCommitTrendData)
            .collect(Collectors.toList());
        
        // 填充缺失的日期数据（值为0）
        trendData = fillMissingDates(trendData, startDate.toLocalDate(), endDate.toLocalDate());
        
        logger.info("Retrieved {} commit trend data points", trendData.size());
        return trendData;
    }
    
    /**
     * 获取分支活跃度统计
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分支活跃度统计列表
     */
    public List<BranchActivityStats> getBranchActivityStats(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting branch activity stats: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);
        
        List<Object[]> rawStats = commitRepository.getBranchActivityStats(projectId, startDate, endDate);
        
        List<BranchActivityStats> stats = rawStats.stream()
            .map(this::mapToBranchActivityStats)
            .collect(Collectors.toList());
        
        logger.info("Retrieved {} branch activity stats", stats.size());
        return stats;
    }
    
    /**
     * 获取开发者活跃度（按小时分布）
     * 
     * @param developerId 开发者ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 开发者活跃度数据（24小时分布）
     */
    public List<DeveloperActivityData> getDeveloperActivityByHour(String developerId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting developer activity by hour: developerId={}, startDate={}, endDate={}", developerId, startDate, endDate);
        
        List<Object[]> rawActivity = commitRepository.getDeveloperActivityByHour(developerId, startDate, endDate);
        
        // 创建24小时的活跃度数据，初始值为0
        Map<Integer, Integer> activityMap = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            activityMap.put(hour, 0);
        }
        
        // 填充实际数据
        for (Object[] row : rawActivity) {
            Integer hour = (Integer) row[0];
            Long commitCount = (Long) row[1];
            activityMap.put(hour, commitCount.intValue());
        }
        
        List<DeveloperActivityData> activityData = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            DeveloperActivityData data = new DeveloperActivityData();
            data.setHour(hour);
            data.setCommitCount(activityMap.get(hour));
            activityData.add(data);
        }
        
        logger.info("Retrieved developer activity data for 24 hours");
        return activityData;
    }
    
    /**
     * 获取大型提交列表
     * 
     * @param threshold 变更行数阈值
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 大型提交列表
     */
    public List<Commit> getLargeCommits(Integer threshold, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting large commits: threshold={}, startDate={}, endDate={}", threshold, startDate, endDate);
        
        List<Commit> largeCommits = commitRepository.findLargeCommits(threshold, startDate, endDate);
        
        logger.info("Retrieved {} large commits", largeCommits.size());
        return largeCommits;
    }
    
    /**
     * 获取开发者平均提交大小统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 开发者平均提交大小统计列表
     */
    public List<DeveloperAverageCommitSize> getDeveloperAverageCommitSize(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting developer average commit size: startDate={}, endDate={}", startDate, endDate);
        
        List<Object[]> rawStats = commitRepository.getDeveloperAverageCommitSize(startDate, endDate);
        
        List<DeveloperAverageCommitSize> stats = rawStats.stream()
            .map(this::mapToDeveloperAverageCommitSize)
            .collect(Collectors.toList());
        
        logger.info("Retrieved {} developer average commit size stats", stats.size());
        return stats;
    }
    
    /**
     * 获取项目总体统计
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 项目总体统计
     */
    public ProjectTotalStats getProjectTotalStats(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting project total stats: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);
        
        Object[] rawStats = commitRepository.getProjectTotalStats(projectId, startDate, endDate);
        
        ProjectTotalStats stats = mapToProjectTotalStats(rawStats);
        
        logger.info("Retrieved project total stats: commits={}, linesAdded={}, linesDeleted={}", 
                   stats.getTotalCommits(), stats.getTotalLinesAdded(), stats.getTotalLinesDeleted());
        return stats;
    }
    
    // 映射方法
    
    private DeveloperCommitStats mapToDeveloperCommitStats(Object[] row) {
        DeveloperCommitStats stats = new DeveloperCommitStats();
        stats.setDeveloperId((String) row[0]);
        stats.setDeveloperName((String) row[1]);
        stats.setCommitCount(((Long) row[2]).intValue());
        stats.setLinesAdded(((Long) row[3]).intValue());
        stats.setLinesDeleted(((Long) row[4]).intValue());
        stats.setFilesChanged(((Long) row[5]).intValue());
        return stats;
    }
    
    private ProjectCommitStats mapToProjectCommitStats(Object[] row) {
        ProjectCommitStats stats = new ProjectCommitStats();
        stats.setProjectId((String) row[0]);
        stats.setCommitCount(((Long) row[1]).intValue());
        stats.setLinesAdded(((Long) row[2]).intValue());
        stats.setLinesDeleted(((Long) row[3]).intValue());
        stats.setFilesChanged(((Long) row[4]).intValue());
        return stats;
    }
    
    private CommitTrendData mapToCommitTrendData(Object[] row) {
        CommitTrendData data = new CommitTrendData();
        if (row.length == 5) {
            // 包含项目ID的查询结果
            data.setProjectId((String) row[0]);
            data.setDate((LocalDate) row[1]);
            data.setCommitCount(((Long) row[2]).intValue());
            data.setLinesAdded(((Long) row[3]).intValue());
            data.setLinesDeleted(((Long) row[4]).intValue());
        } else {
            // 不包含项目ID的查询结果
            data.setDate((LocalDate) row[0]);
            data.setCommitCount(((Long) row[1]).intValue());
            data.setLinesAdded(((Long) row[2]).intValue());
            data.setLinesDeleted(((Long) row[3]).intValue());
        }
        return data;
    }
    
    private BranchActivityStats mapToBranchActivityStats(Object[] row) {
        BranchActivityStats stats = new BranchActivityStats();
        stats.setBranchName((String) row[0]);
        stats.setCommitCount(((Long) row[1]).intValue());
        stats.setLinesAdded(((Long) row[2]).intValue());
        stats.setLinesDeleted(((Long) row[3]).intValue());
        return stats;
    }
    
    private DeveloperAverageCommitSize mapToDeveloperAverageCommitSize(Object[] row) {
        DeveloperAverageCommitSize stats = new DeveloperAverageCommitSize();
        stats.setDeveloperId((String) row[0]);
        stats.setDeveloperName((String) row[1]);
        stats.setAverageChangedLines(((Double) row[2]).intValue());
        stats.setAverageChangedFiles(((Double) row[3]).intValue());
        return stats;
    }
    
    private ProjectTotalStats mapToProjectTotalStats(Object[] row) {
        ProjectTotalStats stats = new ProjectTotalStats();
        stats.setTotalLinesAdded(((Long) row[0]).intValue());
        stats.setTotalLinesDeleted(((Long) row[1]).intValue());
        stats.setTotalFilesChanged(((Long) row[2]).intValue());
        stats.setTotalCommits(((Long) row[3]).intValue());
        return stats;
    }
    
    /**
     * 填充缺失的日期数据
     */
    private List<CommitTrendData> fillMissingDates(List<CommitTrendData> trendData, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, CommitTrendData> dataMap = trendData.stream()
            .collect(Collectors.toMap(CommitTrendData::getDate, data -> data));
        
        List<CommitTrendData> filledData = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            CommitTrendData data = dataMap.get(currentDate);
            if (data == null) {
                data = new CommitTrendData();
                data.setDate(currentDate);
                data.setCommitCount(0);
                data.setLinesAdded(0);
                data.setLinesDeleted(0);
            }
            filledData.add(data);
            currentDate = currentDate.plusDays(1);
        }
        
        return filledData;
    }
    
    // 统计数据类
    
    public static class DeveloperCommitStats {
        private String developerId;
        private String developerName;
        private Integer commitCount;
        private Integer linesAdded;
        private Integer linesDeleted;
        private Integer filesChanged;
        
        // Getters and Setters
        public String getDeveloperId() { return developerId; }
        public void setDeveloperId(String developerId) { this.developerId = developerId; }
        
        public String getDeveloperName() { return developerName; }
        public void setDeveloperName(String developerName) { this.developerName = developerName; }
        
        public Integer getCommitCount() { return commitCount; }
        public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
        
        public Integer getLinesAdded() { return linesAdded; }
        public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }
        
        public Integer getLinesDeleted() { return linesDeleted; }
        public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }
        
        public Integer getFilesChanged() { return filesChanged; }
        public void setFilesChanged(Integer filesChanged) { this.filesChanged = filesChanged; }
    }
    
    public static class ProjectCommitStats {
        private String projectId;
        private Integer commitCount;
        private Integer linesAdded;
        private Integer linesDeleted;
        private Integer filesChanged;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public Integer getCommitCount() { return commitCount; }
        public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
        
        public Integer getLinesAdded() { return linesAdded; }
        public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }
        
        public Integer getLinesDeleted() { return linesDeleted; }
        public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }
        
        public Integer getFilesChanged() { return filesChanged; }
        public void setFilesChanged(Integer filesChanged) { this.filesChanged = filesChanged; }
    }
    
    public static class CommitTrendData {
        private String projectId;
        private LocalDate date;
        private Integer commitCount;
        private Integer linesAdded;
        private Integer linesDeleted;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public Integer getCommitCount() { return commitCount; }
        public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
        
        public Integer getLinesAdded() { return linesAdded; }
        public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }
        
        public Integer getLinesDeleted() { return linesDeleted; }
        public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }
    }
    
    public static class BranchActivityStats {
        private String branchName;
        private Integer commitCount;
        private Integer linesAdded;
        private Integer linesDeleted;
        
        // Getters and Setters
        public String getBranchName() { return branchName; }
        public void setBranchName(String branchName) { this.branchName = branchName; }
        
        public Integer getCommitCount() { return commitCount; }
        public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
        
        public Integer getLinesAdded() { return linesAdded; }
        public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }
        
        public Integer getLinesDeleted() { return linesDeleted; }
        public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }
    }
    
    public static class DeveloperActivityData {
        private Integer hour;
        private Integer commitCount;
        
        // Getters and Setters
        public Integer getHour() { return hour; }
        public void setHour(Integer hour) { this.hour = hour; }
        
        public Integer getCommitCount() { return commitCount; }
        public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
    }
    
    public static class DeveloperAverageCommitSize {
        private String developerId;
        private String developerName;
        private Integer averageChangedLines;
        private Integer averageChangedFiles;
        
        // Getters and Setters
        public String getDeveloperId() { return developerId; }
        public void setDeveloperId(String developerId) { this.developerId = developerId; }
        
        public String getDeveloperName() { return developerName; }
        public void setDeveloperName(String developerName) { this.developerName = developerName; }
        
        public Integer getAverageChangedLines() { return averageChangedLines; }
        public void setAverageChangedLines(Integer averageChangedLines) { this.averageChangedLines = averageChangedLines; }
        
        public Integer getAverageChangedFiles() { return averageChangedFiles; }
        public void setAverageChangedFiles(Integer averageChangedFiles) { this.averageChangedFiles = averageChangedFiles; }
    }
    
    public static class ProjectTotalStats {
        private Integer totalCommits;
        private Integer totalLinesAdded;
        private Integer totalLinesDeleted;
        private Integer totalFilesChanged;
        
        // Getters and Setters
        public Integer getTotalCommits() { return totalCommits; }
        public void setTotalCommits(Integer totalCommits) { this.totalCommits = totalCommits; }
        
        public Integer getTotalLinesAdded() { return totalLinesAdded; }
        public void setTotalLinesAdded(Integer totalLinesAdded) { this.totalLinesAdded = totalLinesAdded; }
        
        public Integer getTotalLinesDeleted() { return totalLinesDeleted; }
        public void setTotalLinesDeleted(Integer totalLinesDeleted) { this.totalLinesDeleted = totalLinesDeleted; }
        
        public Integer getTotalFilesChanged() { return totalFilesChanged; }
        public void setTotalFilesChanged(Integer totalFilesChanged) { this.totalFilesChanged = totalFilesChanged; }
    }
}