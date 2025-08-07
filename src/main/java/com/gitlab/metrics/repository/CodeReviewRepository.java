package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码评审Repository接口
 * 提供代码评审相关的数据访问方法，包括评审效率分析和统计查询
 */
@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    
    /**
     * 根据合并请求ID查找代码评审记录
     */
    List<CodeReview> findByMergeRequestIdOrderByReviewedAtDesc(Long mergeRequestId);
    
    /**
     * 根据评审者ID查找代码评审记录
     */
    List<CodeReview> findByReviewerIdOrderByReviewedAtDesc(String reviewerId);
    
    /**
     * 根据评审状态查找代码评审记录
     */
    List<CodeReview> findByStatusOrderByReviewedAtDesc(String status);
    
    /**
     * 根据评审者ID和时间范围查找代码评审记录
     */
    List<CodeReview> findByReviewerIdAndReviewedAtBetween(String reviewerId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计评审者的评审数据
     * 返回：评审者ID, 评审者名称, 评审总数, 批准数量, 要求修改数量, 平均评审时间(小时)
     */
    @Query("SELECT r.reviewerId, r.reviewerName, COUNT(r), " +
           "COUNT(CASE WHEN r.status = 'approved' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'changes_requested' THEN 1 END), " +
           "AVG(CASE WHEN r.submittedAt IS NOT NULL THEN " +
           "TIMESTAMPDIFF(HOUR, r.reviewedAt, r.submittedAt) END) " +
           "FROM CodeReview r WHERE r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.reviewerId, r.reviewerName " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> getReviewerStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计项目的代码评审数据
     */
    @Query("SELECT mr.projectId, COUNT(r), " +
           "COUNT(CASE WHEN r.status = 'approved' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'changes_requested' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'commented' THEN 1 END) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY mr.projectId " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> getProjectReviewStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定项目中评审者的评审数据
     */
    @Query("SELECT r.reviewerId, r.reviewerName, COUNT(r), " +
           "COUNT(CASE WHEN r.status = 'approved' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'changes_requested' THEN 1 END) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.reviewerId, r.reviewerName " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> getReviewerStatsByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计代码评审趋势
     */
    @Query("SELECT DATE(r.reviewedAt), COUNT(r), " +
           "COUNT(CASE WHEN r.status = 'approved' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'changes_requested' THEN 1 END) " +
           "FROM CodeReview r WHERE r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(r.reviewedAt) " +
           "ORDER BY DATE(r.reviewedAt)")
    List<Object[]> getReviewTrendByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目和日期统计代码评审趋势
     */
    @Query("SELECT mr.projectId, DATE(r.reviewedAt), COUNT(r), " +
           "COUNT(CASE WHEN r.status = 'approved' THEN 1 END) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY mr.projectId, DATE(r.reviewedAt) " +
           "ORDER BY DATE(r.reviewedAt)")
    List<Object[]> getReviewTrendByProjectAndDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计评审状态分布
     */
    @Query("SELECT r.status, COUNT(r) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.status")
    List<Object[]> getReviewStatusStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计评审类型分布
     */
    @Query("SELECT r.reviewType, COUNT(r) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.reviewType")
    List<Object[]> getReviewTypeStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最活跃的评审者
     */
    @Query("SELECT r.reviewerId, r.reviewerName, COUNT(r), " +
           "AVG(COALESCE(r.commentsCount, 0)) " +
           "FROM CodeReview r WHERE r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.reviewerId, r.reviewerName " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> getMostActiveReviewers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计必需评审的完成情况
     */
    @Query("SELECT COUNT(r), " +
           "COUNT(CASE WHEN r.isRequired = true THEN 1 END), " +
           "COUNT(CASE WHEN r.isRequired = true AND r.status = 'approved' THEN 1 END) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end")
    Object[] getRequiredReviewStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计评审响应时间
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, mr.createdAt, r.reviewedAt)), " +
           "MIN(TIMESTAMPDIFF(HOUR, mr.createdAt, r.reviewedAt)), " +
           "MAX(TIMESTAMPDIFF(HOUR, mr.createdAt, r.reviewedAt)) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end")
    Object[] getReviewResponseTimeStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找评审响应时间最长的记录
     */
    @Query("SELECT r, TIMESTAMPDIFF(HOUR, mr.createdAt, r.reviewedAt) as responseHours " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId AND r.reviewedAt BETWEEN :start AND :end " +
           "ORDER BY TIMESTAMPDIFF(HOUR, mr.createdAt, r.reviewedAt) DESC")
    List<Object[]> getLongestReviewResponseTimes(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计评审者的平均评论数量
     */
    @Query("SELECT r.reviewerId, r.reviewerName, " +
           "AVG(COALESCE(r.commentsCount, 0)), " +
           "COUNT(r) " +
           "FROM CodeReview r WHERE r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY r.reviewerId, r.reviewerName " +
           "ORDER BY AVG(COALESCE(r.commentsCount, 0)) DESC")
    List<Object[]> getReviewerCommentStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找被拒绝最多的合并请求
     */
    @Query("SELECT mr, COUNT(r) as rejectionCount " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE r.status = 'changes_requested' AND r.reviewedAt BETWEEN :start AND :end " +
           "GROUP BY mr " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> getMostRejectedMergeRequests(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计评审覆盖率（有评审的MR占比）
     */
    @Query("SELECT " +
           "COUNT(DISTINCT mr.id) as totalMRs, " +
           "COUNT(DISTINCT r.mergeRequest.id) as reviewedMRs " +
           "FROM MergeRequest mr LEFT JOIN CodeReview r ON mr.id = r.mergeRequest.id " +
           "WHERE mr.projectId = :projectId AND mr.createdAt BETWEEN :start AND :end")
    Object[] getReviewCoverageStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 比较两个时间段的评审效率
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN r.reviewedAt BETWEEN :period1Start AND :period1End THEN 1 END), " +
           "COUNT(CASE WHEN r.reviewedAt BETWEEN :period2Start AND :period2End THEN 1 END), " +
           "AVG(CASE WHEN r.reviewedAt BETWEEN :period1Start AND :period1End AND r.submittedAt IS NOT NULL " +
           "THEN TIMESTAMPDIFF(HOUR, r.reviewedAt, r.submittedAt) END), " +
           "AVG(CASE WHEN r.reviewedAt BETWEEN :period2Start AND :period2End AND r.submittedAt IS NOT NULL " +
           "THEN TIMESTAMPDIFF(HOUR, r.reviewedAt, r.submittedAt) END) " +
           "FROM CodeReview r JOIN r.mergeRequest mr " +
           "WHERE mr.projectId = :projectId " +
           "AND (r.reviewedAt BETWEEN :period1Start AND :period1End OR r.reviewedAt BETWEEN :period2Start AND :period2End)")
    Object[] compareReviewEfficiencyBetweenPeriods(
        @Param("projectId") String projectId,
        @Param("period1Start") LocalDateTime period1Start, @Param("period1End") LocalDateTime period1End,
        @Param("period2Start") LocalDateTime period2Start, @Param("period2End") LocalDateTime period2End);
}