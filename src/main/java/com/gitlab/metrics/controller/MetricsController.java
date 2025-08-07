package com.gitlab.metrics.controller;

import com.gitlab.metrics.dto.CommitStatsResponse;
import com.gitlab.metrics.dto.DashboardResponse;
import com.gitlab.metrics.dto.QualityMetricsResponse;
import com.gitlab.metrics.dto.TestCoverageResponse;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import com.gitlab.metrics.repository.TestCoverageRepository;
import com.gitlab.metrics.service.CommitStatisticsService;
import com.gitlab.metrics.service.DashboardService;
import com.gitlab.metrics.service.TestCoverageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 核心指标查询API控制器
 * 提供代码提交、质量指标、测试覆盖率等核心指标的查询接口
 */
@RestController
@RequestMapping("/api/v1/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    
    @Autowired
    private CommitStatisticsService commitStatisticsService;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    @Autowired
    private TestCoverageService testCoverageService;
    
    @Autowired
    private DashboardService dashboardService;
    
    /**
     * 获取代码提交统计数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param projectId 项目ID（可选）
     * @param developerId 开发者ID（可选）
     * @param includeDetails 是否包含详细数据
     * @return 代码提交统计响应
     */
    @GetMapping("/commits")
    public ResponseEntity<CommitStatsResponse> getCommitStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String developerId,
            @RequestParam(defaultValue = "true") boolean includeDetails) {
        
        logger.info("获取代码提交统计: startDate={}, endDate={}, projectId={}, developerId={}", 
                   startDate, endDate, projectId, developerId);
        
        try {
            CommitStatsResponse response = new CommitStatsResponse(startDate, endDate);
            response.setProjectId(projectId);
            response.setDeveloperId(developerId);
            
            if (includeDetails) {
                // 获取开发者统计
                List<CommitStatisticsService.DeveloperCommitStats> developerStats = 
                    commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, developerId);
                response.setDeveloperStats(developerStats);
                
                // 获取项目统计（如果没有指定项目ID）
                if (projectId == null) {
                    List<CommitStatisticsService.ProjectCommitStats> projectStats = 
                        commitStatisticsService.getProjectCommitStats(startDate, endDate);
                    response.setProjectStats(projectStats);
                }
                
                // 获取趋势数据
                List<CommitStatisticsService.CommitTrendData> trendData = 
                    commitStatisticsService.getCommitTrend(startDate, endDate, projectId);
                response.setTrendData(trendData);
            }
            
            // 获取总体统计（如果指定了项目ID）
            if (projectId != null) {
                CommitStatisticsService.ProjectTotalStats totalStats = 
                    commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
                response.setTotalStats(totalStats);
            }
            
            logger.info("代码提交统计查询完成");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取代码提交统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取代码质量指标数据
     * 
     * @param projectId 项目ID
     * @param timeRange 时间范围 (7d, 30d, 90d, 180d, 1y)
     * @param startDate 自定义开始日期（可选）
     * @param endDate 自定义结束日期（可选）
     * @return 代码质量指标响应
     */
    @GetMapping("/quality")
    public ResponseEntity<QualityMetricsResponse> getQualityMetrics(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("获取代码质量指标: projectId={}, timeRange={}", projectId, timeRange);
        
        try {
            // 计算时间范围
            LocalDateTime[] dateRange = calculateDateRange(timeRange, startDate, endDate);
            LocalDateTime calculatedStartDate = dateRange[0];
            LocalDateTime calculatedEndDate = dateRange[1];
            
            QualityMetricsResponse response = new QualityMetricsResponse(projectId, timeRange);
            response.setStartDate(calculatedStartDate);
            response.setEndDate(calculatedEndDate);
            
            // 获取质量指标记录
            List<QualityMetrics> qualityMetrics = qualityMetricsRepository
                .findByProjectIdAndTimestampBetween(projectId, calculatedStartDate, calculatedEndDate);
            response.setQualityMetrics(qualityMetrics);
            
            // 获取趋势数据
            Object[] trendResult = qualityMetricsRepository
                .getQualityTrend(projectId, calculatedStartDate, calculatedEndDate);
            if (trendResult != null && trendResult.length >= 4) {
                QualityMetricsResponse.QualityTrendData trendData = new QualityMetricsResponse.QualityTrendData();
                trendData.setAverageComplexity(trendResult[0] != null ? (Double) trendResult[0] : 0.0);
                trendData.setAverageDuplicateRate(trendResult[1] != null ? (Double) trendResult[1] : 0.0);
                trendData.setAverageMaintainabilityIndex(trendResult[2] != null ? (Double) trendResult[2] : 0.0);
                trendData.setAverageTechnicalDebt(trendResult[3] != null ? (Double) trendResult[3] : 0.0);
                
                // 获取按日期的趋势点
                List<Object[]> trendByDate = qualityMetricsRepository
                    .getQualityTrendByDate(projectId, calculatedStartDate, calculatedEndDate);
                List<QualityMetricsResponse.QualityTrendPoint> trendPoints = trendByDate.stream()
                    .map(this::mapToQualityTrendPoint)
                    .collect(java.util.stream.Collectors.toList());
                trendData.setTrendPoints(trendPoints);
                
                response.setTrendData(trendData);
            }
            
            // 获取概览数据
            List<Object[]> qualityGateStats = qualityMetricsRepository
                .getQualityGateStats(projectId, calculatedStartDate, calculatedEndDate);
            QualityMetricsResponse.QualityOverview overview = buildQualityOverview(qualityGateStats, qualityMetrics);
            response.setOverview(overview);
            
            logger.info("代码质量指标查询完成: 记录数={}", qualityMetrics.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取代码质量指标失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取测试覆盖率数据
     * 
     * @param projectId 项目ID
     * @param timeRange 时间范围 (7d, 30d, 90d, 180d, 1y)
     * @param startDate 自定义开始日期（可选）
     * @param endDate 自定义结束日期（可选）
     * @return 测试覆盖率响应
     */
    @GetMapping("/coverage")
    public ResponseEntity<TestCoverageResponse> getTestCoverage(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("获取测试覆盖率: projectId={}, timeRange={}", projectId, timeRange);
        
        try {
            // 计算时间范围
            LocalDateTime[] dateRange = calculateDateRange(timeRange, startDate, endDate);
            LocalDateTime calculatedStartDate = dateRange[0];
            LocalDateTime calculatedEndDate = dateRange[1];
            
            TestCoverageResponse response = new TestCoverageResponse(projectId, timeRange);
            response.setStartDate(calculatedStartDate);
            response.setEndDate(calculatedEndDate);
            
            // 获取覆盖率记录
            List<TestCoverage> coverageRecords = testCoverageRepository
                .findByProjectIdAndTimestampBetween(projectId, calculatedStartDate, calculatedEndDate);
            response.setCoverageRecords(coverageRecords);
            
            // 获取趋势数据
            TestCoverageService.CoverageTrend trendData = testCoverageService
                .getCoverageTrend(projectId, calculatedStartDate, calculatedEndDate);
            response.setTrendData(trendData);
            
            // 获取概览数据
            List<Object[]> coverageStatusStats = testCoverageRepository
                .getCoverageStatusStats(projectId, calculatedStartDate, calculatedEndDate);
            TestCoverageResponse.CoverageOverview overview = buildCoverageOverview(coverageStatusStats, coverageRecords);
            response.setOverview(overview);
            
            logger.info("测试覆盖率查询完成: 记录数={}", coverageRecords.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取测试覆盖率失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取效率看板数据
     * 
     * @param projectId 项目ID
     * @param timeRange 时间范围 (7d, 30d, 90d, 180d, 1y)
     * @param startDate 自定义开始日期（可选）
     * @param endDate 自定义结束日期（可选）
     * @return 效率看板数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("获取效率看板数据: projectId={}, timeRange={}", projectId, timeRange);
        
        try {
            // 计算时间范围
            LocalDateTime[] dateRange = calculateDateRange(timeRange, startDate, endDate);
            LocalDateTime calculatedStartDate = dateRange[0];
            LocalDateTime calculatedEndDate = dateRange[1];
            
            DashboardResponse response = dashboardService.getDashboardData(
                projectId, calculatedStartDate, calculatedEndDate, timeRange);
            
            logger.info("效率看板数据获取完成");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取效率看板数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取项目概览数据
     * 
     * @param projectId 项目ID
     * @param timeRange 时间范围
     * @return 项目概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<Object> getProjectOverview(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "30d") String timeRange) {
        
        logger.info("获取项目概览: projectId={}, timeRange={}", projectId, timeRange);
        
        try {
            LocalDateTime[] dateRange = calculateDateRange(timeRange, null, null);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];
            
            // 构建概览响应
            java.util.Map<String, Object> overview = new java.util.HashMap<>();
            overview.put("projectId", projectId);
            overview.put("timeRange", timeRange);
            overview.put("startDate", startDate);
            overview.put("endDate", endDate);
            
            // 获取提交统计
            CommitStatisticsService.ProjectTotalStats commitStats = 
                commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
            overview.put("commits", commitStats);
            
            // 获取最新质量指标
            List<QualityMetrics> latestQuality = qualityMetricsRepository.findLatestByProject(projectId);
            if (!latestQuality.isEmpty()) {
                overview.put("latestQuality", latestQuality.get(0));
            }
            
            // 获取最新覆盖率
            java.util.Optional<TestCoverage> latestCoverage = testCoverageService.getLatestCoverage(projectId);
            if (latestCoverage.isPresent()) {
                overview.put("latestCoverage", latestCoverage.get());
            }
            
            logger.info("项目概览查询完成");
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("获取项目概览失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 辅助方法
    
    /**
     * 计算日期范围
     */
    private LocalDateTime[] calculateDateRange(String timeRange, LocalDateTime customStartDate, LocalDateTime customEndDate) {
        LocalDateTime endDate = customEndDate != null ? customEndDate : LocalDateTime.now();
        LocalDateTime startDate;
        
        if (customStartDate != null) {
            startDate = customStartDate;
        } else {
            switch (timeRange.toLowerCase()) {
                case "7d":
                    startDate = endDate.minus(7, ChronoUnit.DAYS);
                    break;
                case "30d":
                    startDate = endDate.minus(30, ChronoUnit.DAYS);
                    break;
                case "90d":
                    startDate = endDate.minus(90, ChronoUnit.DAYS);
                    break;
                case "180d":
                    startDate = endDate.minus(180, ChronoUnit.DAYS);
                    break;
                case "1y":
                    startDate = endDate.minus(365, ChronoUnit.DAYS);
                    break;
                default:
                    startDate = endDate.minus(30, ChronoUnit.DAYS);
            }
        }
        
        return new LocalDateTime[]{startDate, endDate};
    }
    
    /**
     * 映射质量趋势点
     */
    private QualityMetricsResponse.QualityTrendPoint mapToQualityTrendPoint(Object[] row) {
        QualityMetricsResponse.QualityTrendPoint point = new QualityMetricsResponse.QualityTrendPoint();
        point.setDate(((LocalDate) row[0]).atStartOfDay());
        point.setComplexity(row[1] != null ? (Double) row[1] : 0.0);
        point.setDuplicateRate(row[2] != null ? (Double) row[2] : 0.0);
        point.setMaintainabilityIndex(row[3] != null ? (Double) row[3] : 0.0);
        point.setTechnicalDebt(row[4] != null ? (Double) row[4] : 0.0);
        return point;
    }
    
    /**
     * 构建质量概览
     */
    private QualityMetricsResponse.QualityOverview buildQualityOverview(
            List<Object[]> qualityGateStats, List<QualityMetrics> qualityMetrics) {
        
        QualityMetricsResponse.QualityOverview overview = new QualityMetricsResponse.QualityOverview();
        
        int totalScans = qualityMetrics.size();
        int passedGates = 0;
        int failedGates = 0;
        int totalBugs = 0;
        int totalVulnerabilities = 0;
        int totalCodeSmells = 0;
        
        // 统计质量门禁状态
        for (Object[] stat : qualityGateStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            if ("PASSED".equals(status)) {
                passedGates = count.intValue();
            } else if ("FAILED".equals(status)) {
                failedGates = count.intValue();
            }
        }
        
        // 统计问题总数
        for (QualityMetrics metric : qualityMetrics) {
            if (metric.getBugs() != null) totalBugs += metric.getBugs();
            if (metric.getVulnerabilities() != null) totalVulnerabilities += metric.getVulnerabilities();
            if (metric.getCodeSmells() != null) totalCodeSmells += metric.getCodeSmells();
        }
        
        overview.setTotalScans(totalScans);
        overview.setPassedGates(passedGates);
        overview.setFailedGates(failedGates);
        overview.setPassRate(totalScans > 0 ? (double) passedGates / totalScans * 100 : 0.0);
        overview.setTotalBugs(totalBugs);
        overview.setTotalVulnerabilities(totalVulnerabilities);
        overview.setTotalCodeSmells(totalCodeSmells);
        
        return overview;
    }
    
    /**
     * 构建覆盖率概览
     */
    private TestCoverageResponse.CoverageOverview buildCoverageOverview(
            List<Object[]> coverageStatusStats, List<TestCoverage> coverageRecords) {
        
        TestCoverageResponse.CoverageOverview overview = new TestCoverageResponse.CoverageOverview();
        
        int totalReports = coverageRecords.size();
        int passedReports = 0;
        int failedReports = 0;
        
        // 统计状态
        for (Object[] stat : coverageStatusStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            if ("PASSED".equals(status)) {
                passedReports = count.intValue();
            } else if ("FAILED".equals(status)) {
                failedReports = count.intValue();
            }
        }
        
        // 计算平均覆盖率
        double totalLineCoverage = 0;
        double totalBranchCoverage = 0;
        double totalFunctionCoverage = 0;
        int validRecords = 0;
        
        for (TestCoverage coverage : coverageRecords) {
            if (coverage.getLineCoverage() != null) {
                totalLineCoverage += coverage.getLineCoverage();
                validRecords++;
            }
            if (coverage.getBranchCoverage() != null) {
                totalBranchCoverage += coverage.getBranchCoverage();
            }
            if (coverage.getFunctionCoverage() != null) {
                totalFunctionCoverage += coverage.getFunctionCoverage();
            }
        }
        
        overview.setTotalReports(totalReports);
        overview.setPassedReports(passedReports);
        overview.setFailedReports(failedReports);
        overview.setPassRate(totalReports > 0 ? (double) passedReports / totalReports * 100 : 0.0);
        overview.setAverageLineCoverage(validRecords > 0 ? totalLineCoverage / validRecords : 0.0);
        overview.setAverageBranchCoverage(validRecords > 0 ? totalBranchCoverage / validRecords : 0.0);
        overview.setAverageFunctionCoverage(validRecords > 0 ? totalFunctionCoverage / validRecords : 0.0);
        
        return overview;
    }
    
    /**
     * 获取多项目对比数据
     * 
     * @param projectIds 项目ID列表（逗号分隔）
     * @param timeRange 时间范围
     * @return 多项目对比数据
     */
    @GetMapping("/compare")
    public ResponseEntity<Object> compareProjects(
            @RequestParam String projectIds,
            @RequestParam(defaultValue = "30d") String timeRange) {
        
        logger.info("获取多项目对比数据: projectIds={}, timeRange={}", projectIds, timeRange);
        
        try {
            String[] projectIdArray = projectIds.split(",");
            LocalDateTime[] dateRange = calculateDateRange(timeRange, null, null);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];
            
            java.util.Map<String, Object> comparison = new java.util.HashMap<>();
            comparison.put("timeRange", timeRange);
            comparison.put("startDate", startDate);
            comparison.put("endDate", endDate);
            
            java.util.List<java.util.Map<String, Object>> projectComparisons = new java.util.ArrayList<>();
            
            for (String projectId : projectIdArray) {
                projectId = projectId.trim();
                java.util.Map<String, Object> projectData = new java.util.HashMap<>();
                projectData.put("projectId", projectId);
                
                // 获取基础统计
                CommitStatisticsService.ProjectTotalStats commitStats = 
                    commitStatisticsService.getProjectTotalStats(projectId, startDate, endDate);
                projectData.put("commits", commitStats.getTotalCommits());
                projectData.put("linesAdded", commitStats.getTotalLinesAdded());
                projectData.put("linesDeleted", commitStats.getTotalLinesDeleted());
                
                // 获取开发者数量
                List<CommitStatisticsService.DeveloperCommitStats> developerStats = 
                    commitStatisticsService.getDeveloperCommitStats(startDate, endDate, projectId, null);
                projectData.put("developers", developerStats.size());
                
                // 获取最新质量指标
                List<QualityMetrics> latestQuality = qualityMetricsRepository.findLatestByProject(projectId);
                if (!latestQuality.isEmpty()) {
                    QualityMetrics quality = latestQuality.get(0);
                    projectData.put("qualityGate", quality.getQualityGate());
                    projectData.put("bugs", quality.getBugs());
                    projectData.put("vulnerabilities", quality.getVulnerabilities());
                }
                
                // 获取最新覆盖率
                java.util.Optional<TestCoverage> latestCoverage = testCoverageService.getLatestCoverage(projectId);
                if (latestCoverage.isPresent()) {
                    projectData.put("testCoverage", latestCoverage.get().getLineCoverage());
                }
                
                projectComparisons.add(projectData);
            }
            
            comparison.put("projects", projectComparisons);
            
            logger.info("多项目对比数据获取完成");
            return ResponseEntity.ok(comparison);
            
        } catch (Exception e) {
            logger.error("获取多项目对比数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取团队效率排行榜
     * 
     * @param timeRange 时间范围
     * @param limit 返回数量限制
     * @return 团队效率排行榜
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<Object> getLeaderboard(
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("获取团队效率排行榜: timeRange={}, limit={}", timeRange, limit);
        
        try {
            LocalDateTime[] dateRange = calculateDateRange(timeRange, null, null);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];
            
            java.util.Map<String, Object> leaderboard = new java.util.HashMap<>();
            leaderboard.put("timeRange", timeRange);
            leaderboard.put("startDate", startDate);
            leaderboard.put("endDate", endDate);
            
            // 获取所有开发者统计
            List<CommitStatisticsService.DeveloperCommitStats> allDeveloperStats = 
                commitStatisticsService.getDeveloperCommitStats(startDate, endDate, null, null);
            
            // 按提交数量排序
            List<java.util.Map<String, Object>> commitLeaders = allDeveloperStats.stream()
                .sorted((a, b) -> Integer.compare(b.getCommitCount(), a.getCommitCount()))
                .limit(limit)
                .map(this::convertToLeaderboardEntry)
                .collect(java.util.stream.Collectors.toList());
            leaderboard.put("commitLeaders", commitLeaders);
            
            // 按代码行数排序
            List<java.util.Map<String, Object>> codeLeaders = allDeveloperStats.stream()
                .sorted((a, b) -> Integer.compare(
                    b.getLinesAdded() + b.getLinesDeleted(), 
                    a.getLinesAdded() + a.getLinesDeleted()))
                .limit(limit)
                .map(this::convertToLeaderboardEntry)
                .collect(java.util.stream.Collectors.toList());
            leaderboard.put("codeLeaders", codeLeaders);
            
            // 按文件修改数排序
            List<java.util.Map<String, Object>> fileLeaders = allDeveloperStats.stream()
                .sorted((a, b) -> Integer.compare(b.getFilesChanged(), a.getFilesChanged()))
                .limit(limit)
                .map(this::convertToLeaderboardEntry)
                .collect(java.util.stream.Collectors.toList());
            leaderboard.put("fileLeaders", fileLeaders);
            
            logger.info("团队效率排行榜获取完成");
            return ResponseEntity.ok(leaderboard);
            
        } catch (Exception e) {
            logger.error("获取团队效率排行榜失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取实时统计数据
     * 
     * @return 实时统计数据
     */
    @GetMapping("/realtime")
    public ResponseEntity<Object> getRealtimeStats() {
        logger.info("获取实时统计数据");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            
            java.util.Map<String, Object> realtimeStats = new java.util.HashMap<>();
            realtimeStats.put("timestamp", now);
            realtimeStats.put("date", todayStart.toLocalDate());
            
            // 今日提交统计
            List<CommitStatisticsService.ProjectCommitStats> todayCommits = 
                commitStatisticsService.getProjectCommitStats(todayStart, now);
            int totalTodayCommits = todayCommits.stream()
                .mapToInt(CommitStatisticsService.ProjectCommitStats::getCommitCount)
                .sum();
            realtimeStats.put("todayCommits", totalTodayCommits);
            
            // 活跃项目数
            realtimeStats.put("activeProjects", todayCommits.size());
            
            // 今日活跃开发者
            List<CommitStatisticsService.DeveloperCommitStats> todayDevelopers = 
                commitStatisticsService.getDeveloperCommitStats(todayStart, now, null, null);
            realtimeStats.put("activeDevelopers", todayDevelopers.size());
            
            // 最近的质量门禁失败
            List<QualityMetrics> recentFailures = qualityMetricsRepository
                .findFailedQualityGates(todayStart, now);
            realtimeStats.put("qualityGateFailures", recentFailures.size());
            
            // 最近的覆盖率失败
            List<TestCoverage> coverageFailures = testCoverageRepository
                .findFailedCoverageRecords(todayStart, now);
            realtimeStats.put("coverageFailures", coverageFailures.size());
            
            logger.info("实时统计数据获取完成");
            return ResponseEntity.ok(realtimeStats);
            
        } catch (Exception e) {
            logger.error("获取实时统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 辅助方法
    
    private java.util.Map<String, Object> convertToLeaderboardEntry(CommitStatisticsService.DeveloperCommitStats stats) {
        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("developerId", stats.getDeveloperId());
        entry.put("developerName", stats.getDeveloperName());
        entry.put("commits", stats.getCommitCount());
        entry.put("linesAdded", stats.getLinesAdded());
        entry.put("linesDeleted", stats.getLinesDeleted());
        entry.put("filesChanged", stats.getFilesChanged());
        entry.put("totalLines", stats.getLinesAdded() + stats.getLinesDeleted());
        return entry;
    }
}