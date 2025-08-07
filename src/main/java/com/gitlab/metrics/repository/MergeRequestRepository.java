package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.MergeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 合并请求Repository接口
 * 提供合并请求相关的数据访问方法，包括代码评审效率分析和统计查询
 */
@Repository
public interface MergeRequestRepository extends JpaRepository<MergeRequest, Long> {
    
    /**
     * 根据MR ID查找合并请求
     */
    Optional<MergeRequest> findByMrId(String mrId);
    
    /**
     * 根据项目ID查找合并请求
     */
    List<MergeRequest> findByProjectIdOrderByCreatedAtDesc(String projectId);
    
    /**
     * 根据作者ID查找合并请求
     */
    List<MergeRequest> findByAuthorIdOrderByCreatedAtDesc(String authorId);
    
    /**
     * 根据状态查找合并请求
     */
    List<MergeRequest> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * 根据项目ID和时间范围查找合并请求
     */
    List<MergeRequest> findByProjectIdAndCreatedAtBetween(String projectId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据作者ID和时间范围查找合并请求
     */
    List<MergeRequest> findByAuthorIdAndCreatedAtBetween(String authorId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计开发者的合并请求数据
     * 返回：作者ID, 作者名称, MR总数, 已合并数量, 平均合并时间(小时)
     */
    @Query("SELECT m.authorId, m.authorName, COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END), " +
           "AVG(CASE WHEN m.mergedAt IS NOT NULL THEN " +
           "TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt) END) " +
           "FROM MergeRequest m WHERE m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.authorId, m.authorName " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> getDeveloperMergeRequestStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计项目的合并请求数据
     */
    @Query("SELECT m.projectId, COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END), " +
           "COUNT(CASE WHEN m.status = 'closed' THEN 1 END), " +
           "COUNT(CASE WHEN m.status = 'opened' THEN 1 END) " +
           "FROM MergeRequest m WHERE m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.projectId " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> getProjectMergeRequestStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定项目中开发者的合并请求数据
     */
    @Query("SELECT m.authorId, m.authorName, COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END), " +
           "AVG(CASE WHEN m.mergedAt IS NOT NULL THEN " +
           "TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt) END) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.authorId, m.authorName " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> getDeveloperMergeRequestStatsByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计合并请求趋势
     */
    @Query("SELECT DATE(m.createdAt), COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END), " +
           "COUNT(CASE WHEN m.status = 'closed' THEN 1 END) " +
           "FROM MergeRequest m WHERE m.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(m.createdAt) " +
           "ORDER BY DATE(m.createdAt)")
    List<Object[]> getMergeRequestTrendByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目和日期统计合并请求趋势
     */
    @Query("SELECT m.projectId, DATE(m.createdAt), COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.projectId, DATE(m.createdAt) " +
           "ORDER BY DATE(m.createdAt)")
    List<Object[]> getMergeRequestTrendByProjectAndDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计合并请求的平均处理时间
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt)), " +
           "MIN(TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt)), " +
           "MAX(TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt)) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.status = 'merged' " +
           "AND m.createdAt BETWEEN :start AND :end")
    Object[] getMergeRequestProcessingTimeStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找长时间未处理的合并请求
     */
    @Query("SELECT m FROM MergeRequest m WHERE m.status = 'opened' " +
           "AND TIMESTAMPDIFF(HOUR, m.createdAt, CURRENT_TIMESTAMP) > :hoursThreshold " +
           "ORDER BY m.createdAt ASC")
    List<MergeRequest> findLongPendingMergeRequests(@Param("hoursThreshold") Integer hoursThreshold);
    
    /**
     * 按项目查找长时间未处理的合并请求
     */
    @Query("SELECT m FROM MergeRequest m WHERE m.projectId = :projectId AND m.status = 'opened' " +
           "AND TIMESTAMPDIFF(HOUR, m.createdAt, CURRENT_TIMESTAMP) > :hoursThreshold " +
           "ORDER BY m.createdAt ASC")
    List<MergeRequest> findLongPendingMergeRequestsByProject(
        @Param("projectId") String projectId, @Param("hoursThreshold") Integer hoursThreshold);
    
    /**
     * 统计大型合并请求（代码变更量大）
     */
    @Query("SELECT m FROM MergeRequest m WHERE (COALESCE(m.additions, 0) + COALESCE(m.deletions, 0)) > :threshold " +
           "AND m.createdAt BETWEEN :start AND :end " +
           "ORDER BY (COALESCE(m.additions, 0) + COALESCE(m.deletions, 0)) DESC")
    List<MergeRequest> findLargeMergeRequests(
        @Param("threshold") Integer threshold, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计开发者的平均合并请求大小
     */
    @Query("SELECT m.authorId, m.authorName, " +
           "AVG(COALESCE(m.additions, 0) + COALESCE(m.deletions, 0)), " +
           "AVG(COALESCE(m.changedFiles, 0)) " +
           "FROM MergeRequest m WHERE m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.authorId, m.authorName " +
           "ORDER BY AVG(COALESCE(m.additions, 0) + COALESCE(m.deletions, 0)) DESC")
    List<Object[]> getDeveloperAverageMergeRequestSize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计分支合并活跃度
     */
    @Query("SELECT m.targetBranch, COUNT(m), " +
           "COUNT(CASE WHEN m.status = 'merged' THEN 1 END) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.targetBranch " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> getBranchMergeActivity(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计合并请求状态分布
     */
    @Query("SELECT m.status, COUNT(m) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.createdAt BETWEEN :start AND :end " +
           "GROUP BY m.status")
    List<Object[]> getMergeRequestStatusStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最活跃的合并者
     */
    @Query("SELECT m.mergedById, m.mergedByName, COUNT(m) " +
           "FROM MergeRequest m WHERE m.status = 'merged' AND m.mergedById IS NOT NULL " +
           "AND m.mergedAt BETWEEN :start AND :end " +
           "GROUP BY m.mergedById, m.mergedByName " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> getMostActiveMergers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计工作进行中的合并请求
     */
    @Query("SELECT COUNT(m) FROM MergeRequest m WHERE m.workInProgress = true AND m.status = 'opened' " +
           "AND m.projectId = :projectId")
    Long countWorkInProgressMergeRequests(@Param("projectId") String projectId);
    
    /**
     * 统计合并请求的评审覆盖率
     */
    @Query("SELECT COUNT(m), " +
           "COUNT(CASE WHEN SIZE(m.reviews) > 0 THEN 1 END), " +
           "AVG(SIZE(m.reviews)) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId AND m.createdAt BETWEEN :start AND :end")
    Object[] getReviewCoverageStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 比较两个时间段的合并请求效率
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN m.createdAt BETWEEN :period1Start AND :period1End THEN 1 END), " +
           "COUNT(CASE WHEN m.createdAt BETWEEN :period2Start AND :period2End THEN 1 END), " +
           "AVG(CASE WHEN m.createdAt BETWEEN :period1Start AND :period1End AND m.mergedAt IS NOT NULL " +
           "THEN TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt) END), " +
           "AVG(CASE WHEN m.createdAt BETWEEN :period2Start AND :period2End AND m.mergedAt IS NOT NULL " +
           "THEN TIMESTAMPDIFF(HOUR, m.createdAt, m.mergedAt) END) " +
           "FROM MergeRequest m WHERE m.projectId = :projectId " +
           "AND (m.createdAt BETWEEN :period1Start AND :period1End OR m.createdAt BETWEEN :period2Start AND :period2End)")
    Object[] compareMergeRequestEfficiencyBetweenPeriods(
        @Param("projectId") String projectId,
        @Param("period1Start") LocalDateTime period1Start, @Param("period1End") LocalDateTime period1End,
        @Param("period2Start") LocalDateTime period2Start, @Param("period2End") LocalDateTime period2End);
}