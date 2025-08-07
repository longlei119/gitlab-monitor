package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.TestCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 测试覆盖率Repository接口
 * 提供测试覆盖率相关的数据访问方法，包括覆盖率趋势分析和统计查询
 */
@Repository
public interface TestCoverageRepository extends JpaRepository<TestCoverage, Long> {
    
    /**
     * 根据提交SHA查找测试覆盖率
     */
    Optional<TestCoverage> findByCommitSha(String commitSha);
    
    /**
     * 根据项目ID查找测试覆盖率，按时间倒序
     */
    List<TestCoverage> findByProjectIdOrderByTimestampDesc(String projectId);
    
    /**
     * 根据项目ID和时间范围查找测试覆盖率
     */
    List<TestCoverage> findByProjectIdAndTimestampBetween(String projectId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找最新的测试覆盖率记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.projectId = :projectId ORDER BY t.timestamp DESC")
    List<TestCoverage> findLatestByProject(@Param("projectId") String projectId);
    
    /**
     * 获取项目的覆盖率趋势分析
     * 返回：平均行覆盖率、平均分支覆盖率、平均函数覆盖率
     */
    @Query("SELECT AVG(t.lineCoverage), AVG(t.branchCoverage), AVG(t.functionCoverage) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.timestamp BETWEEN :start AND :end")
    Object[] getCoverageTrend(@Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计覆盖率趋势
     */
    @Query("SELECT DATE(t.timestamp), AVG(t.lineCoverage), AVG(t.branchCoverage), AVG(t.functionCoverage) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(t.timestamp) " +
           "ORDER BY DATE(t.timestamp)")
    List<Object[]> getCoverageTrendByDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找覆盖率低于阈值的记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.projectId = :projectId AND t.lineCoverage < :threshold " +
           "AND t.timestamp BETWEEN :start AND :end ORDER BY t.lineCoverage ASC")
    List<TestCoverage> findLowCoverageRecords(
        @Param("projectId") String projectId, @Param("threshold") Double threshold, 
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计覆盖率状态分布
     */
    @Query("SELECT t.status, COUNT(t) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.timestamp BETWEEN :start AND :end " +
           "GROUP BY t.status")
    List<Object[]> getCoverageStatusStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找覆盖率失败的记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.status = 'FAILED' AND t.timestamp BETWEEN :start AND :end " +
           "ORDER BY t.timestamp DESC")
    List<TestCoverage> findFailedCoverageRecords(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目查找覆盖率失败的记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.projectId = :projectId AND t.status = 'FAILED' " +
           "AND t.timestamp BETWEEN :start AND :end ORDER BY t.timestamp DESC")
    List<TestCoverage> findFailedCoverageRecordsByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计测试覆盖率详细信息
     */
    @Query("SELECT AVG(t.lineCoverage), AVG(t.branchCoverage), AVG(t.functionCoverage), " +
           "AVG(t.totalLines), AVG(t.coveredLines), AVG(t.totalBranches), AVG(t.coveredBranches) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.timestamp BETWEEN :start AND :end")
    Object[] getCoverageDetailStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按报告类型统计覆盖率
     */
    @Query("SELECT t.reportType, COUNT(t), AVG(t.lineCoverage), AVG(t.branchCoverage) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.timestamp BETWEEN :start AND :end " +
           "GROUP BY t.reportType")
    List<Object[]> getCoverageStatsByReportType(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找覆盖率最高的记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.projectId = :projectId AND t.lineCoverage IS NOT NULL " +
           "AND t.timestamp BETWEEN :start AND :end ORDER BY t.lineCoverage DESC")
    List<TestCoverage> findHighestCoverage(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找覆盖率最低的记录
     */
    @Query("SELECT t FROM TestCoverage t WHERE t.projectId = :projectId AND t.lineCoverage IS NOT NULL " +
           "AND t.timestamp BETWEEN :start AND :end ORDER BY t.lineCoverage ASC")
    List<TestCoverage> findLowestCoverage(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 比较两个时间段的覆盖率
     */
    @Query("SELECT " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period1Start AND :period1End THEN t.lineCoverage END), " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period2Start AND :period2End THEN t.lineCoverage END), " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period1Start AND :period1End THEN t.branchCoverage END), " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period2Start AND :period2End THEN t.branchCoverage END), " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period1Start AND :period1End THEN t.functionCoverage END), " +
           "AVG(CASE WHEN t.timestamp BETWEEN :period2Start AND :period2End THEN t.functionCoverage END) " +
           "FROM TestCoverage t WHERE t.projectId = :projectId " +
           "AND (t.timestamp BETWEEN :period1Start AND :period1End OR t.timestamp BETWEEN :period2Start AND :period2End)")
    Object[] compareCoverageBetweenPeriods(
        @Param("projectId") String projectId,
        @Param("period1Start") LocalDateTime period1Start, @Param("period1End") LocalDateTime period1End,
        @Param("period2Start") LocalDateTime period2Start, @Param("period2End") LocalDateTime period2End);
    
    /**
     * 获取所有项目的覆盖率概览
     */
    @Query("SELECT t.projectId, COUNT(t), AVG(t.lineCoverage), AVG(t.branchCoverage), AVG(t.functionCoverage) " +
           "FROM TestCoverage t WHERE t.timestamp BETWEEN :start AND :end " +
           "GROUP BY t.projectId " +
           "ORDER BY AVG(t.lineCoverage) DESC")
    List<Object[]> getAllProjectsCoverageOverview(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找覆盖率改善最明显的项目
     */
    @Query("SELECT t.projectId, " +
           "AVG(CASE WHEN t.timestamp BETWEEN :oldStart AND :oldEnd THEN t.lineCoverage END) as oldCoverage, " +
           "AVG(CASE WHEN t.timestamp BETWEEN :newStart AND :newEnd THEN t.lineCoverage END) as newCoverage " +
           "FROM TestCoverage t WHERE t.lineCoverage IS NOT NULL " +
           "AND (t.timestamp BETWEEN :oldStart AND :oldEnd OR t.timestamp BETWEEN :newStart AND :newEnd) " +
           "GROUP BY t.projectId " +
           "HAVING oldCoverage IS NOT NULL AND newCoverage IS NOT NULL " +
           "ORDER BY (newCoverage - oldCoverage) DESC")
    List<Object[]> findMostImprovedCoverageProjects(
        @Param("oldStart") LocalDateTime oldStart, @Param("oldEnd") LocalDateTime oldEnd,
        @Param("newStart") LocalDateTime newStart, @Param("newEnd") LocalDateTime newEnd);
    
    /**
     * 统计覆盖率阈值达标情况
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN t.lineCoverage >= t.threshold THEN 1 END) as passed, " +
           "COUNT(CASE WHEN t.lineCoverage < t.threshold THEN 1 END) as failed, " +
           "COUNT(t) as total " +
           "FROM TestCoverage t WHERE t.projectId = :projectId AND t.threshold IS NOT NULL " +
           "AND t.timestamp BETWEEN :start AND :end")
    Object[] getCoverageThresholdStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}