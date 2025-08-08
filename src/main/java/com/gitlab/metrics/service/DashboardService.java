package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.DashboardResponse;
import com.gitlab.metrics.entity.Issue;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 效率看板服务
 * 提供综合的开发效率看板数据，包括趋势分析和对比数据计算
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    @Autowired
    private CommitRepository commitRepository;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    @Autowired
    private MergeRequestRepository mergeRequestRepository;
    
    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private CommitStatisticsService commitStatisticsService;
    
    @Autowired
    private BugFixEfficiencyService bugFixEfficiencyService;
    
    /**
     * 获取项目效率看板数据
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param timeRange 时间范围标识
     * @return 效率看板数据
     */
    @Cacheable(value = "dashboard", key = "#projectId + '_' + #timeRange", unless = "#result == null")
    public DashboardResponse getDashboardData(String projectId, LocalDateTime startDate, 
                                            LocalDateTime endDate, String timeRange) {
        logger.info("获取效率看板数据: projectId={}, timeRange={}", projectId, timeRange);
        
        DashboardResponse response = new DashboardResponse(projectId, timeRange);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        
        try {
            // 获取总体指标
            DashboardResponse.OverallMetrics overallMetrics = buildOverallMetrics(projectId, startDate, endDate);
            response.setOverallMetrics(overallMetrics);
            
            // 获取开发者效率
            DashboardResponse.DeveloperEfficiency developerEfficiency = buildDeveloperEfficiency(projectId, startDate, endDate);
            response.setDeveloperEfficiency(developerEfficiency);
            
            // 获取质量趋势
            DashboardResponse.QualityTrends qualityTrends = buildQualityTrends(projectId, startDate, endDate);
            response.setQualityTrends(qualityTrends);
            
            // 获取生产力指标
            DashboardResponse.ProductivityMetrics productivityMetrics = buildProductivityMetrics(projectId, startDate, endDate);
            response.setProductivityMetrics(productivityMetrics);
            
            // 获取趋势数据
            List<DashboardResponse.TrendPoint> trendData = buildTrendData(projectId, startDate, endDate);
            response.setTrendData(trendData);
            
            logger.info("效率看板数据获取完成");
            return response;
            
        } catch (Exception e) {
            logger.error("获取效率看板数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取效率看板数据失败", e);
        }
    }
    
    /**
     * 构建总体指标
     */
    private DashboardResponse.OverallMetrics buildOverallMetrics(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        DashboardResponse.OverallMetrics metrics = new DashboardResponse.OverallMetrics();
        
        // 获取提交统计
        CommitStatisticsService.ProjectTotalStats commitStats = 
            commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
        
        metrics.setTotalCommits(commitStats.getTotalCommits());
        metrics.setTotalLinesAdded(commitStats.getTotalLinesAdded());
        metrics.setTotalLinesDeleted(commitStats.getTotalLinesDeleted());
        
        // 计算平均提交大小
        if (commitStats.getTotalCommits() > 0) {
            double avgSize = (double) (commitStats.getTotalLinesAdded() + commitStats.getTotalLinesDeleted()) 
                           / commitStats.getTotalCommits();
            metrics.setAverageCommitSize(avgSize);
        } else {
            metrics.setAverageCommitSize(0.0);
        }
        
        // 计算代码流失率
        if (commitStats.getTotalLinesAdded() > 0) {
            double churnRate = (double) commitStats.getTotalLinesDeleted() / commitStats.getTotalLinesAdded() * 100;
            metrics.setCodeChurnRate(churnRate);
        } else {
            metrics.setCodeChurnRate(0.0);
        }
        
        // 获取开发者数量
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = 
            commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null);
        metrics.setTotalDevelopers(developerStats.size());
        
        // 获取合并请求数量
        List<MergeRequest> mergeRequests = mergeRequestRepository
            .findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate);
        metrics.setTotalMergeRequests(mergeRequests.size());
        
        // 获取Issue数量
        List<Issue> issues = issueRepository
            .findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate);
        metrics.setTotalIssues(issues.size());
        
        return metrics;
    }
    
    /**
     * 构建开发者效率
     */
    private DashboardResponse.DeveloperEfficiency buildDeveloperEfficiency(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        DashboardResponse.DeveloperEfficiency efficiency = new DashboardResponse.DeveloperEfficiency();
        
        // 获取开发者统计
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = 
            commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null);
        
        // 转换为DeveloperMetric并计算效率分数
        List<DashboardResponse.DeveloperMetric> topDevelopers = developerStats.stream()
            .map(this::convertToDeveloperMetric)
            .sorted((a, b) -> Double.compare(b.getEfficiency(), a.getEfficiency()))
            .limit(10)
            .collect(Collectors.toList());
        efficiency.setTopDevelopers(topDevelopers);
        
        // 计算平均值
        if (!developerStats.isEmpty()) {
            double avgCommits = developerStats.stream()
                .mapToInt(CommitStatisticsService.DeveloperCommitStats::getCommitCount)
                .average().orElse(0.0);
            efficiency.setAverageCommitsPerDeveloper(avgCommits);
            
            double avgLines = developerStats.stream()
                .mapToInt(stat -> stat.getLinesAdded() + stat.getLinesDeleted())
                .average().orElse(0.0);
            efficiency.setAverageLinesPerDeveloper(avgLines);
        }
        
        // 构建开发者分布
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("高产出", (int) developerStats.stream().filter(s -> s.getCommitCount() > 20).count());
        distribution.put("中等产出", (int) developerStats.stream().filter(s -> s.getCommitCount() >= 10 && s.getCommitCount() <= 20).count());
        distribution.put("低产出", (int) developerStats.stream().filter(s -> s.getCommitCount() < 10).count());
        efficiency.setDeveloperDistribution(distribution);
        
        return efficiency;
    }
    
    /**
     * 构建质量趋势
     */
    private DashboardResponse.QualityTrends buildQualityTrends(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        DashboardResponse.QualityTrends trends = new DashboardResponse.QualityTrends();
        
        // 获取当前期间的质量指标
        List<QualityMetrics> currentMetrics = qualityMetricsRepository
            .findByProjectIdAndTimestampBetween(projectId, startDate, endDate);
        
        if (!currentMetrics.isEmpty()) {
            double currentScore = calculateQualityScore(currentMetrics);
            trends.setCurrentQualityScore(currentScore);
            
            // 获取前一个期间的质量指标进行对比
            long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            LocalDateTime prevStart = startDate.minus(daysDiff, ChronoUnit.DAYS);
            LocalDateTime prevEnd = startDate;
            
            List<QualityMetrics> previousMetrics = qualityMetricsRepository
                .findByProjectIdAndTimestampBetween(projectId, prevStart, prevEnd);
            
            if (!previousMetrics.isEmpty()) {
                double previousScore = calculateQualityScore(previousMetrics);
                trends.setPreviousQualityScore(previousScore);
                trends.setQualityChange(currentScore - previousScore);
            }
        }
        
        // 获取Bug统计
        List<Issue> bugs = issueRepository.findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate);
        // 过滤出Bug类型的Issue
        bugs = bugs.stream()
            .filter(issue -> "bug".equals(issue.getIssueType()))
            .collect(Collectors.toList());
        trends.setTotalBugs(bugs.size());
        
        List<Issue> fixedBugs = bugs.stream()
            .filter(bug -> "closed".equals(bug.getStatus()))
            .collect(Collectors.toList());
        trends.setFixedBugs(fixedBugs.size());
        
        if (!bugs.isEmpty()) {
            trends.setBugFixRate((double) fixedBugs.size() / bugs.size() * 100);
        }
        
        // 计算平均修复时间
        if (!fixedBugs.isEmpty()) {
            double avgFixTime = fixedBugs.stream()
                .filter(bug -> bug.getClosedAt() != null)
                .mapToLong(bug -> ChronoUnit.HOURS.between(bug.getCreatedAt(), bug.getClosedAt()))
                .average().orElse(0.0);
            trends.setAverageFixTime(avgFixTime);
        }
        
        // 获取测试覆盖率
        List<TestCoverage> coverageRecords = testCoverageRepository
            .findByProjectIdAndTimestampBetween(projectId, startDate, endDate);
        
        if (!coverageRecords.isEmpty()) {
            double avgCoverage = coverageRecords.stream()
                .filter(c -> c.getLineCoverage() != null)
                .mapToDouble(TestCoverage::getLineCoverage)
                .average().orElse(0.0);
            trends.setTestCoverage(avgCoverage);
            
            // 计算覆盖率变化
            long coverageDaysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            LocalDateTime coveragePrevStart = startDate.minus(coverageDaysDiff, ChronoUnit.DAYS);
            
            List<TestCoverage> prevCoverageRecords = testCoverageRepository
                .findByProjectIdAndTimestampBetween(projectId, coveragePrevStart, startDate);
            
            if (!prevCoverageRecords.isEmpty()) {
                double prevAvgCoverage = prevCoverageRecords.stream()
                    .filter(c -> c.getLineCoverage() != null)
                    .mapToDouble(TestCoverage::getLineCoverage)
                    .average().orElse(0.0);
                trends.setTestCoverageChange(avgCoverage - prevAvgCoverage);
            }
        }
        
        return trends;
    }
    
    /**
     * 构建生产力指标
     */
    private DashboardResponse.ProductivityMetrics buildProductivityMetrics(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        DashboardResponse.ProductivityMetrics metrics = new DashboardResponse.ProductivityMetrics();
        
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysDiff == 0) daysDiff = 1; // 避免除零
        
        // 获取提交统计
        CommitStatisticsService.ProjectTotalStats commitStats = 
            commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
        
        metrics.setCommitsPerDay((double) commitStats.getTotalCommits() / daysDiff);
        metrics.setLinesPerDay((double) (commitStats.getTotalLinesAdded() + commitStats.getTotalLinesDeleted()) / daysDiff);
        
        // 获取合并请求统计
        List<MergeRequest> mergeRequests = mergeRequestRepository
            .findByProjectIdAndCreatedAtBetween(projectId, startDate, endDate);
        metrics.setMergeRequestsPerDay((double) mergeRequests.size() / daysDiff);
        
        // 计算平均合并时间
        List<MergeRequest> mergedRequests = mergeRequests.stream()
            .filter(mr -> mr.getMergedAt() != null)
            .collect(Collectors.toList());
        
        if (!mergedRequests.isEmpty()) {
            double avgMergeTime = mergedRequests.stream()
                .mapToLong(mr -> ChronoUnit.HOURS.between(mr.getCreatedAt(), mr.getMergedAt()))
                .average().orElse(0.0);
            metrics.setAverageMergeTime(avgMergeTime);
        }
        
        // 计算代码评审效率（基于评审时间和质量）
        double reviewEfficiency = calculateCodeReviewEfficiency(mergedRequests);
        metrics.setCodeReviewEfficiency(reviewEfficiency);
        
        // 获取活跃贡献者数量
        List<CommitStatisticsService.DeveloperCommitStats> developerStats = 
            commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null);
        int activeContributors = (int) developerStats.stream()
            .filter(stat -> stat.getCommitCount() > 0)
            .count();
        metrics.setActiveContributors(activeContributors);
        
        return metrics;
    }
    
    /**
     * 构建趋势数据
     */
    private List<DashboardResponse.TrendPoint> buildTrendData(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        List<DashboardResponse.TrendPoint> trendPoints = new ArrayList<>();
        
        // 获取提交趋势
        List<CommitStatisticsService.CommitTrendData> commitTrend = 
            commitStatisticsService.getCommitTrend(startDate, endDate, projectId);
        
        // 获取质量趋势
        List<Object[]> qualityTrend = qualityMetricsRepository
            .getQualityTrendByDate(projectId, startDate, endDate);
        Map<String, Double> qualityByDate = qualityTrend.stream()
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> calculateQualityScoreFromRow(row),
                (existing, replacement) -> existing
            ));
        
        // 获取覆盖率趋势
        List<Object[]> coverageTrend = testCoverageRepository
            .getCoverageTrendByDate(projectId, startDate, endDate);
        Map<String, Double> coverageByDate = coverageTrend.stream()
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> row[1] != null ? (Double) row[1] : 0.0,
                (existing, replacement) -> existing
            ));
        
        // 合并趋势数据
        for (CommitStatisticsService.CommitTrendData commitData : commitTrend) {
            DashboardResponse.TrendPoint point = new DashboardResponse.TrendPoint();
            point.setDate(commitData.getDate().atStartOfDay());
            point.setCommits(commitData.getCommitCount());
            point.setLinesAdded(commitData.getLinesAdded());
            point.setLinesDeleted(commitData.getLinesDeleted());
            
            String dateKey = commitData.getDate().toString();
            point.setQualityScore(qualityByDate.getOrDefault(dateKey, 0.0));
            point.setTestCoverage(coverageByDate.getOrDefault(dateKey, 0.0));
            
            trendPoints.add(point);
        }
        
        return trendPoints;
    }
    
    // 辅助方法
    
    /**
     * 转换为开发者指标
     */
    private DashboardResponse.DeveloperMetric convertToDeveloperMetric(CommitStatisticsService.DeveloperCommitStats stats) {
        DashboardResponse.DeveloperMetric metric = new DashboardResponse.DeveloperMetric();
        metric.setDeveloperId(stats.getDeveloperId());
        metric.setDeveloperName(stats.getDeveloperName());
        metric.setCommits(stats.getCommitCount());
        metric.setLinesAdded(stats.getLinesAdded());
        metric.setLinesDeleted(stats.getLinesDeleted());
        metric.setFilesChanged(stats.getFilesChanged());
        
        // 计算效率分数（基于提交数量、代码行数和文件数的综合评分）
        double efficiency = calculateDeveloperEfficiency(stats);
        metric.setEfficiency(efficiency);
        
        return metric;
    }
    
    /**
     * 计算开发者效率分数
     */
    private double calculateDeveloperEfficiency(CommitStatisticsService.DeveloperCommitStats stats) {
        // 基于提交频率、代码质量（行数/提交比）、文件覆盖度的综合评分
        double commitScore = Math.min(stats.getCommitCount() * 2.0, 100.0); // 提交数量分数，最高100
        double codeQualityScore = stats.getCommitCount() > 0 ? 
            Math.min((double)(stats.getLinesAdded() + stats.getLinesDeleted()) / stats.getCommitCount() / 10.0, 50.0) : 0.0;
        double fileScore = Math.min(stats.getFilesChanged() * 1.0, 50.0); // 文件修改分数，最高50
        
        return (commitScore * 0.5 + codeQualityScore * 0.3 + fileScore * 0.2);
    }
    
    /**
     * 计算质量分数
     */
    private double calculateQualityScore(List<QualityMetrics> metrics) {
        if (metrics.isEmpty()) return 0.0;
        
        double totalScore = 0.0;
        int count = 0;
        
        for (QualityMetrics metric : metrics) {
            double score = 100.0; // 基础分数
            
            // 根据各项指标扣分
            if (metric.getBugs() != null) {
                score -= metric.getBugs() * 2.0; // 每个bug扣2分
            }
            if (metric.getVulnerabilities() != null) {
                score -= metric.getVulnerabilities() * 5.0; // 每个漏洞扣5分
            }
            if (metric.getCodeSmells() != null) {
                score -= metric.getCodeSmells() * 0.5; // 每个代码异味扣0.5分
            }
            if (metric.getDuplicateRate() != null) {
                score -= metric.getDuplicateRate() * 0.5; // 重复率扣分
            }
            
            totalScore += Math.max(score, 0.0); // 确保分数不为负
            count++;
        }
        
        return count > 0 ? totalScore / count : 0.0;
    }
    
    /**
     * 从查询结果行计算质量分数
     */
    private double calculateQualityScoreFromRow(Object[] row) {
        // row[1] = complexity, row[2] = duplicateRate, row[3] = maintainabilityIndex, row[4] = technicalDebt
        double score = 100.0;
        
        if (row[1] != null) {
            Double complexity = (Double) row[1];
            score -= complexity * 0.1; // 复杂度扣分
        }
        if (row[2] != null) {
            Double duplicateRate = (Double) row[2];
            score -= duplicateRate * 0.5; // 重复率扣分
        }
        if (row[3] != null) {
            Double maintainabilityIndex = (Double) row[3];
            score = score * (maintainabilityIndex / 100.0); // 可维护性指数调整
        }
        
        return Math.max(score, 0.0);
    }
    
    /**
     * 计算代码评审效率
     */
    private double calculateCodeReviewEfficiency(List<MergeRequest> mergedRequests) {
        if (mergedRequests.isEmpty()) return 0.0;
        
        double totalEfficiency = 0.0;
        int count = 0;
        
        for (MergeRequest mr : mergedRequests) {
            if (mr.getMergedAt() != null) {
                long reviewTime = ChronoUnit.HOURS.between(mr.getCreatedAt(), mr.getMergedAt());
                // 评审效率 = 100 - (评审时间 / 24) * 10，最低为0
                double efficiency = Math.max(100.0 - (reviewTime / 24.0) * 10.0, 0.0);
                totalEfficiency += efficiency;
                count++;
            }
        }
        
        return count > 0 ? totalEfficiency / count : 0.0;
    }
}