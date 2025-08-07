package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.QualityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码质量指标Repository接口
 * 提供代码质量相关的数据访问方法，包括质量趋势分析和统计查询
 */
@Repository
public interface QualityMetricsRepository extends JpaRepository<QualityMetrics, Long> {
    
    /**
     * 根据提交SHA查找质量指标
     */
    Optional<QualityMetrics> findByCommitSha(String commitSha);
    
    /**
     * 根据项目ID查找质量指标，按时间倒序
     */
    List<QualityMetrics> findByProjectIdOrderByTimestampDesc(String projectId);
    
    /**
     * 根据项目ID和时间范围查找质量指标
     */
    List<QualityMetrics> findByProjectIdAndTimestampBetween(String projectId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找最新的质量指标记录
     */
    @Query("SELECT q FROM QualityMetrics q WHERE q.projectId = :projectId ORDER BY q.timestamp DESC")
    List<QualityMetrics> findLatestByProject(@Param("projectId") String projectId);
    
    /**
     * 获取项目的质量趋势分析
     * 返回：平均复杂度、平均重复率、平均可维护性指数、平均技术债务
     */
    @Query("SELECT AVG(q.codeComplexity), AVG(q.duplicateRate), AVG(q.maintainabilityIndex), AVG(q.technicalDebt) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.timestamp BETWEEN :start AND :end")
    Object[] getQualityTrend(@Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计质量趋势
     */
    @Query("SELECT DATE(q.timestamp), AVG(q.codeComplexity), AVG(q.duplicateRate), " +
           "AVG(q.maintainabilityIndex), AVG(q.technicalDebt) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(q.timestamp) " +
           "ORDER BY DATE(q.timestamp)")
    List<Object[]> getQualityTrendByDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计安全问题趋势
     */
    @Query("SELECT DATE(q.timestamp), AVG(q.securityIssues), AVG(q.vulnerabilities), AVG(q.hotspots) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(q.timestamp) " +
           "ORDER BY DATE(q.timestamp)")
    List<Object[]> getSecurityTrendByDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计代码问题趋势
     */
    @Query("SELECT DATE(q.timestamp), AVG(q.bugs), AVG(q.codeSmells), AVG(q.performanceIssues) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(q.timestamp) " +
           "ORDER BY DATE(q.timestamp)")
    List<Object[]> getCodeIssueTrendByDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找质量门禁失败的记录
     */
    @Query("SELECT q FROM QualityMetrics q WHERE q.qualityGate = 'FAILED' AND q.timestamp BETWEEN :start AND :end " +
           "ORDER BY q.timestamp DESC")
    List<QualityMetrics> findFailedQualityGates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目查找质量门禁失败的记录
     */
    @Query("SELECT q FROM QualityMetrics q WHERE q.projectId = :projectId AND q.qualityGate = 'FAILED' " +
           "AND q.timestamp BETWEEN :start AND :end ORDER BY q.timestamp DESC")
    List<QualityMetrics> findFailedQualityGatesByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计质量门禁通过率
     */
    @Query("SELECT q.qualityGate, COUNT(q) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.timestamp BETWEEN :start AND :end " +
           "GROUP BY q.qualityGate")
    List<Object[]> getQualityGateStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找代码复杂度最高的记录
     */
    @Query("SELECT q FROM QualityMetrics q WHERE q.projectId = :projectId AND q.codeComplexity IS NOT NULL " +
           "AND q.timestamp BETWEEN :start AND :end ORDER BY q.codeComplexity DESC")
    List<QualityMetrics> findHighestComplexity(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找重复率最高的记录
     */
    @Query("SELECT q FROM QualityMetrics q WHERE q.projectId = :projectId AND q.duplicateRate IS NOT NULL " +
           "AND q.timestamp BETWEEN :start AND :end ORDER BY q.duplicateRate DESC")
    List<QualityMetrics> findHighestDuplication(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计技术债务总量
     */
    @Query("SELECT SUM(q.technicalDebt), AVG(q.technicalDebt), MAX(q.technicalDebt) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId AND q.technicalDebt IS NOT NULL " +
           "AND q.timestamp BETWEEN :start AND :end")
    Object[] getTechnicalDebtStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 比较两个时间段的质量指标
     */
    @Query("SELECT " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period1Start AND :period1End THEN q.codeComplexity END), " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period2Start AND :period2End THEN q.codeComplexity END), " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period1Start AND :period1End THEN q.duplicateRate END), " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period2Start AND :period2End THEN q.duplicateRate END), " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period1Start AND :period1End THEN q.maintainabilityIndex END), " +
           "AVG(CASE WHEN q.timestamp BETWEEN :period2Start AND :period2End THEN q.maintainabilityIndex END) " +
           "FROM QualityMetrics q WHERE q.projectId = :projectId " +
           "AND (q.timestamp BETWEEN :period1Start AND :period1End OR q.timestamp BETWEEN :period2Start AND :period2End)")
    Object[] compareQualityBetweenPeriods(
        @Param("projectId") String projectId,
        @Param("period1Start") LocalDateTime period1Start, @Param("period1End") LocalDateTime period1End,
        @Param("period2Start") LocalDateTime period2Start, @Param("period2End") LocalDateTime period2End);
    
    /**
     * 获取所有项目的质量概览
     */
    @Query("SELECT q.projectId, COUNT(q), AVG(q.codeComplexity), AVG(q.duplicateRate), " +
           "AVG(q.maintainabilityIndex), SUM(q.technicalDebt) " +
           "FROM QualityMetrics q WHERE q.timestamp BETWEEN :start AND :end " +
           "GROUP BY q.projectId " +
           "ORDER BY AVG(q.maintainabilityIndex) DESC")
    List<Object[]> getAllProjectsQualityOverview(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找质量改善最明显的项目
     */
    @Query("SELECT q.projectId, " +
           "AVG(CASE WHEN q.timestamp BETWEEN :oldStart AND :oldEnd THEN q.maintainabilityIndex END) as oldIndex, " +
           "AVG(CASE WHEN q.timestamp BETWEEN :newStart AND :newEnd THEN q.maintainabilityIndex END) as newIndex " +
           "FROM QualityMetrics q WHERE q.maintainabilityIndex IS NOT NULL " +
           "AND (q.timestamp BETWEEN :oldStart AND :oldEnd OR q.timestamp BETWEEN :newStart AND :newEnd) " +
           "GROUP BY q.projectId " +
           "HAVING oldIndex IS NOT NULL AND newIndex IS NOT NULL " +
           "ORDER BY (newIndex - oldIndex) DESC")
    List<Object[]> findMostImprovedProjects(
        @Param("oldStart") LocalDateTime oldStart, @Param("oldEnd") LocalDateTime oldEnd,
        @Param("newStart") LocalDateTime newStart, @Param("newEnd") LocalDateTime newEnd);
}