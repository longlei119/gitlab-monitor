package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码质量指标服务
 * 提供代码质量指标的业务逻辑处理，包括质量趋势分析和统计查询
 */
@Service
@Transactional
public class QualityMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityMetricsService.class);
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    /**
     * 获取项目的质量指标历史记录
     */
    @Transactional(readOnly = true)
    public List<QualityMetrics> getQualityHistory(String projectId) {
        return qualityMetricsRepository.findByProjectIdOrderByTimestampDesc(projectId);
    }
    
    /**
     * 获取项目的最新质量指标
     */
    @Transactional(readOnly = true)
    public Optional<QualityMetrics> getLatestQualityMetrics(String projectId) {
        List<QualityMetrics> metrics = qualityMetricsRepository.findLatestByProject(projectId);
        return metrics.isEmpty() ? Optional.empty() : Optional.of(metrics.get(0));
    }
    
    /**
     * 获取指定时间范围内的质量指标记录
     */
    @Transactional(readOnly = true)
    public List<QualityMetrics> getQualityMetricsByDateRange(String projectId, LocalDateTime start, LocalDateTime end) {
        return qualityMetricsRepository.findByProjectIdAndTimestampBetween(projectId, start, end);
    }
    
    /**
     * 获取质量趋势分析
     */
    @Transactional(readOnly = true)
    public QualityTrend getQualityTrend(String projectId, LocalDateTime start, LocalDateTime end) {
        Object[] result = qualityMetricsRepository.getQualityTrend(projectId, start, end);
        
        if (result != null && result.length >= 4) {
            QualityTrend trend = new QualityTrend();
            trend.setProjectId(projectId);
            trend.setStartDate(start);
            trend.setEndDate(end);
            trend.setAverageComplexity(result[0] != null ? (Double) result[0] : 0.0);
            trend.setAverageDuplicateRate(result[1] != null ? (Double) result[1] : 0.0);
            trend.setAverageMaintainabilityIndex(result[2] != null ? (Double) result[2] : 0.0);
            trend.setAverageTechnicalDebt(result[3] != null ? (Double) result[3] : 0.0);
            return trend;
        }
        
        return new QualityTrend(projectId, start, end, 0.0, 0.0, 0.0, 0.0);
    }
    
    /**
     * 获取质量门禁失败的记录
     */
    @Transactional(readOnly = true)
    public List<QualityMetrics> getFailedQualityGates(String projectId, LocalDateTime start, LocalDateTime end) {
        if (projectId != null) {
            return qualityMetricsRepository.findFailedQualityGatesByProject(projectId, start, end);
        } else {
            return qualityMetricsRepository.findFailedQualityGates(start, end);
        }
    }
    
    /**
     * 获取质量门禁统计
     */
    @Transactional(readOnly = true)
    public QualityGateStats getQualityGateStats(String projectId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> stats = qualityMetricsRepository.getQualityGateStats(projectId, start, end);
        
        QualityGateStats gateStats = new QualityGateStats();
        gateStats.setProjectId(projectId);
        gateStats.setStartDate(start);
        gateStats.setEndDate(end);
        
        int totalGates = 0;
        int passedGates = 0;
        int failedGates = 0;
        
        for (Object[] stat : stats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            totalGates += count.intValue();
            
            if ("PASSED".equals(status)) {
                passedGates = count.intValue();
            } else if ("FAILED".equals(status)) {
                failedGates = count.intValue();
            }
        }
        
        gateStats.setTotalGates(totalGates);
        gateStats.setPassedGates(passedGates);
        gateStats.setFailedGates(failedGates);
        gateStats.setPassRate(totalGates > 0 ? (double) passedGates / totalGates * 100 : 0.0);
        
        return gateStats;
    }
    
    /**
     * 获取技术债务统计
     */
    @Transactional(readOnly = true)
    public TechnicalDebtStats getTechnicalDebtStats(String projectId, LocalDateTime start, LocalDateTime end) {
        Object[] result = qualityMetricsRepository.getTechnicalDebtStats(projectId, start, end);
        
        TechnicalDebtStats debtStats = new TechnicalDebtStats();
        debtStats.setProjectId(projectId);
        debtStats.setStartDate(start);
        debtStats.setEndDate(end);
        
        if (result != null && result.length >= 3) {
            debtStats.setTotalDebt(result[0] != null ? (Double) result[0] : 0.0);
            debtStats.setAverageDebt(result[1] != null ? (Double) result[1] : 0.0);
            debtStats.setMaxDebt(result[2] != null ? (Double) result[2] : 0.0);
        }
        
        return debtStats;
    }
    
    /**
     * 比较两个时间段的质量指标
     */
    @Transactional(readOnly = true)
    public QualityComparison compareQualityBetweenPeriods(String projectId,
                                                         LocalDateTime period1Start, LocalDateTime period1End,
                                                         LocalDateTime period2Start, LocalDateTime period2End) {
        Object[] result = qualityMetricsRepository.compareQualityBetweenPeriods(
            projectId, period1Start, period1End, period2Start, period2End);
        
        QualityComparison comparison = new QualityComparison();
        comparison.setProjectId(projectId);
        comparison.setPeriod1Start(period1Start);
        comparison.setPeriod1End(period1End);
        comparison.setPeriod2Start(period2Start);
        comparison.setPeriod2End(period2End);
        
        if (result != null && result.length >= 6) {
            comparison.setPeriod1Complexity(result[0] != null ? (Double) result[0] : 0.0);
            comparison.setPeriod2Complexity(result[1] != null ? (Double) result[1] : 0.0);
            comparison.setPeriod1DuplicateRate(result[2] != null ? (Double) result[2] : 0.0);
            comparison.setPeriod2DuplicateRate(result[3] != null ? (Double) result[3] : 0.0);
            comparison.setPeriod1MaintainabilityIndex(result[4] != null ? (Double) result[4] : 0.0);
            comparison.setPeriod2MaintainabilityIndex(result[5] != null ? (Double) result[5] : 0.0);
            
            // 计算变化百分比
            comparison.setComplexityChange(calculatePercentageChange(
                comparison.getPeriod1Complexity(), comparison.getPeriod2Complexity()));
            comparison.setDuplicateRateChange(calculatePercentageChange(
                comparison.getPeriod1DuplicateRate(), comparison.getPeriod2DuplicateRate()));
            comparison.setMaintainabilityIndexChange(calculatePercentageChange(
                comparison.getPeriod1MaintainabilityIndex(), comparison.getPeriod2MaintainabilityIndex()));
        }
        
        return comparison;
    }
    
    /**
     * 获取所有项目的质量概览
     */
    @Transactional(readOnly = true)
    public List<ProjectQualityOverview> getAllProjectsQualityOverview(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = qualityMetricsRepository.getAllProjectsQualityOverview(start, end);
        
        return results.stream()
            .map(this::mapToProjectQualityOverview)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 查找质量改善最明显的项目
     */
    @Transactional(readOnly = true)
    public List<ProjectImprovement> findMostImprovedProjects(
            LocalDateTime oldStart, LocalDateTime oldEnd,
            LocalDateTime newStart, LocalDateTime newEnd) {
        List<Object[]> results = qualityMetricsRepository.findMostImprovedProjects(
            oldStart, oldEnd, newStart, newEnd);
        
        return results.stream()
            .map(this::mapToProjectImprovement)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // 辅助方法
    
    private double calculatePercentageChange(Double oldValue, Double newValue) {
        if (oldValue == null || newValue == null || oldValue == 0) {
            return 0.0;
        }
        return ((newValue - oldValue) / oldValue) * 100;
    }
    
    private ProjectQualityOverview mapToProjectQualityOverview(Object[] row) {
        ProjectQualityOverview overview = new ProjectQualityOverview();
        overview.setProjectId((String) row[0]);
        overview.setTotalScans(((Long) row[1]).intValue());
        overview.setAverageComplexity(row[2] != null ? (Double) row[2] : 0.0);
        overview.setAverageDuplicateRate(row[3] != null ? (Double) row[3] : 0.0);
        overview.setAverageMaintainabilityIndex(row[4] != null ? (Double) row[4] : 0.0);
        overview.setTotalTechnicalDebt(row[5] != null ? (Double) row[5] : 0.0);
        return overview;
    }
    
    private ProjectImprovement mapToProjectImprovement(Object[] row) {
        ProjectImprovement improvement = new ProjectImprovement();
        improvement.setProjectId((String) row[0]);
        improvement.setOldMaintainabilityIndex(row[1] != null ? (Double) row[1] : 0.0);
        improvement.setNewMaintainabilityIndex(row[2] != null ? (Double) row[2] : 0.0);
        improvement.setImprovement(improvement.getNewMaintainabilityIndex() - improvement.getOldMaintainabilityIndex());
        return improvement;
    }
    
    // 数据类
    
    public static class QualityTrend {
        private String projectId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double averageComplexity;
        private Double averageDuplicateRate;
        private Double averageMaintainabilityIndex;
        private Double averageTechnicalDebt;
        
        public QualityTrend() {}
        
        public QualityTrend(String projectId, LocalDateTime startDate, LocalDateTime endDate,
                           Double averageComplexity, Double averageDuplicateRate,
                           Double averageMaintainabilityIndex, Double averageTechnicalDebt) {
            this.projectId = projectId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.averageComplexity = averageComplexity;
            this.averageDuplicateRate = averageDuplicateRate;
            this.averageMaintainabilityIndex = averageMaintainabilityIndex;
            this.averageTechnicalDebt = averageTechnicalDebt;
        }
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        
        public Double getAverageComplexity() { return averageComplexity; }
        public void setAverageComplexity(Double averageComplexity) { this.averageComplexity = averageComplexity; }
        
        public Double getAverageDuplicateRate() { return averageDuplicateRate; }
        public void setAverageDuplicateRate(Double averageDuplicateRate) { this.averageDuplicateRate = averageDuplicateRate; }
        
        public Double getAverageMaintainabilityIndex() { return averageMaintainabilityIndex; }
        public void setAverageMaintainabilityIndex(Double averageMaintainabilityIndex) { this.averageMaintainabilityIndex = averageMaintainabilityIndex; }
        
        public Double getAverageTechnicalDebt() { return averageTechnicalDebt; }
        public void setAverageTechnicalDebt(Double averageTechnicalDebt) { this.averageTechnicalDebt = averageTechnicalDebt; }
    }
    
    public static class QualityGateStats {
        private String projectId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer totalGates;
        private Integer passedGates;
        private Integer failedGates;
        private Double passRate;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        
        public Integer getTotalGates() { return totalGates; }
        public void setTotalGates(Integer totalGates) { this.totalGates = totalGates; }
        
        public Integer getPassedGates() { return passedGates; }
        public void setPassedGates(Integer passedGates) { this.passedGates = passedGates; }
        
        public Integer getFailedGates() { return failedGates; }
        public void setFailedGates(Integer failedGates) { this.failedGates = failedGates; }
        
        public Double getPassRate() { return passRate; }
        public void setPassRate(Double passRate) { this.passRate = passRate; }
    }
    
    public static class TechnicalDebtStats {
        private String projectId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double totalDebt;
        private Double averageDebt;
        private Double maxDebt;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        
        public Double getTotalDebt() { return totalDebt; }
        public void setTotalDebt(Double totalDebt) { this.totalDebt = totalDebt; }
        
        public Double getAverageDebt() { return averageDebt; }
        public void setAverageDebt(Double averageDebt) { this.averageDebt = averageDebt; }
        
        public Double getMaxDebt() { return maxDebt; }
        public void setMaxDebt(Double maxDebt) { this.maxDebt = maxDebt; }
    }
    
    public static class QualityComparison {
        private String projectId;
        private LocalDateTime period1Start;
        private LocalDateTime period1End;
        private LocalDateTime period2Start;
        private LocalDateTime period2End;
        private Double period1Complexity;
        private Double period2Complexity;
        private Double period1DuplicateRate;
        private Double period2DuplicateRate;
        private Double period1MaintainabilityIndex;
        private Double period2MaintainabilityIndex;
        private Double complexityChange;
        private Double duplicateRateChange;
        private Double maintainabilityIndexChange;
        
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
        
        public Double getPeriod1Complexity() { return period1Complexity; }
        public void setPeriod1Complexity(Double period1Complexity) { this.period1Complexity = period1Complexity; }
        
        public Double getPeriod2Complexity() { return period2Complexity; }
        public void setPeriod2Complexity(Double period2Complexity) { this.period2Complexity = period2Complexity; }
        
        public Double getPeriod1DuplicateRate() { return period1DuplicateRate; }
        public void setPeriod1DuplicateRate(Double period1DuplicateRate) { this.period1DuplicateRate = period1DuplicateRate; }
        
        public Double getPeriod2DuplicateRate() { return period2DuplicateRate; }
        public void setPeriod2DuplicateRate(Double period2DuplicateRate) { this.period2DuplicateRate = period2DuplicateRate; }
        
        public Double getPeriod1MaintainabilityIndex() { return period1MaintainabilityIndex; }
        public void setPeriod1MaintainabilityIndex(Double period1MaintainabilityIndex) { this.period1MaintainabilityIndex = period1MaintainabilityIndex; }
        
        public Double getPeriod2MaintainabilityIndex() { return period2MaintainabilityIndex; }
        public void setPeriod2MaintainabilityIndex(Double period2MaintainabilityIndex) { this.period2MaintainabilityIndex = period2MaintainabilityIndex; }
        
        public Double getComplexityChange() { return complexityChange; }
        public void setComplexityChange(Double complexityChange) { this.complexityChange = complexityChange; }
        
        public Double getDuplicateRateChange() { return duplicateRateChange; }
        public void setDuplicateRateChange(Double duplicateRateChange) { this.duplicateRateChange = duplicateRateChange; }
        
        public Double getMaintainabilityIndexChange() { return maintainabilityIndexChange; }
        public void setMaintainabilityIndexChange(Double maintainabilityIndexChange) { this.maintainabilityIndexChange = maintainabilityIndexChange; }
    }
    
    public static class ProjectQualityOverview {
        private String projectId;
        private Integer totalScans;
        private Double averageComplexity;
        private Double averageDuplicateRate;
        private Double averageMaintainabilityIndex;
        private Double totalTechnicalDebt;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public Integer getTotalScans() { return totalScans; }
        public void setTotalScans(Integer totalScans) { this.totalScans = totalScans; }
        
        public Double getAverageComplexity() { return averageComplexity; }
        public void setAverageComplexity(Double averageComplexity) { this.averageComplexity = averageComplexity; }
        
        public Double getAverageDuplicateRate() { return averageDuplicateRate; }
        public void setAverageDuplicateRate(Double averageDuplicateRate) { this.averageDuplicateRate = averageDuplicateRate; }
        
        public Double getAverageMaintainabilityIndex() { return averageMaintainabilityIndex; }
        public void setAverageMaintainabilityIndex(Double averageMaintainabilityIndex) { this.averageMaintainabilityIndex = averageMaintainabilityIndex; }
        
        public Double getTotalTechnicalDebt() { return totalTechnicalDebt; }
        public void setTotalTechnicalDebt(Double totalTechnicalDebt) { this.totalTechnicalDebt = totalTechnicalDebt; }
    }
    
    public static class ProjectImprovement {
        private String projectId;
        private Double oldMaintainabilityIndex;
        private Double newMaintainabilityIndex;
        private Double improvement;
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public Double getOldMaintainabilityIndex() { return oldMaintainabilityIndex; }
        public void setOldMaintainabilityIndex(Double oldMaintainabilityIndex) { this.oldMaintainabilityIndex = oldMaintainabilityIndex; }
        
        public Double getNewMaintainabilityIndex() { return newMaintainabilityIndex; }
        public void setNewMaintainabilityIndex(Double newMaintainabilityIndex) { this.newMaintainabilityIndex = newMaintainabilityIndex; }
        
        public Double getImprovement() { return improvement; }
        public void setImprovement(Double improvement) { this.improvement = improvement; }
    }
}