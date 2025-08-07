package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 问题（Issue）Repository接口
 * 提供Issue相关的数据访问方法，包括Bug修复效率分析和统计查询
 */
@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    
    /**
     * 根据Issue ID查找问题
     */
    Optional<Issue> findByIssueId(String issueId);
    
    /**
     * 根据项目ID查找问题
     */
    List<Issue> findByProjectIdOrderByCreatedAtDesc(String projectId);
    
    /**
     * 根据分配人ID查找问题
     */
    List<Issue> findByAssigneeIdOrderByCreatedAtDesc(String assigneeId);
    
    /**
     * 根据状态查找问题
     */
    List<Issue> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * 根据问题类型查找问题
     */
    List<Issue> findByIssueTypeOrderByCreatedAtDesc(String issueType);
    
    /**
     * 根据项目ID和时间范围查找问题
     */
    List<Issue> findByProjectIdAndCreatedAtBetween(String projectId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据分配人ID和时间范围查找问题
     */
    List<Issue> findByAssigneeIdAndCreatedAtBetween(String assigneeId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计开发者的Issue处理数据
     * 返回：分配人ID, 分配人名称, Issue总数, 已关闭数量, 平均解决时间(小时)
     */
    @Query("SELECT i.assigneeId, i.assigneeName, COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END), " +
           "AVG(CASE WHEN i.resolutionTimeMinutes IS NOT NULL THEN i.resolutionTimeMinutes / 60.0 END) " +
           "FROM Issue i WHERE i.assigneeId IS NOT NULL AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.assigneeId, i.assigneeName " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getDeveloperIssueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计项目的Issue数据
     */
    @Query("SELECT i.projectId, COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END), " +
           "COUNT(CASE WHEN i.status = 'opened' THEN 1 END), " +
           "COUNT(CASE WHEN i.issueType = 'bug' THEN 1 END) " +
           "FROM Issue i WHERE i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.projectId " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getProjectIssueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定项目中开发者的Issue处理数据
     */
    @Query("SELECT i.assigneeId, i.assigneeName, COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END), " +
           "AVG(CASE WHEN i.resolutionTimeMinutes IS NOT NULL THEN i.resolutionTimeMinutes / 60.0 END) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.assigneeId IS NOT NULL " +
           "AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.assigneeId, i.assigneeName " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getDeveloperIssueStatsByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计Issue趋势
     */
    @Query("SELECT DATE(i.createdAt), COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END), " +
           "COUNT(CASE WHEN i.issueType = 'bug' THEN 1 END) " +
           "FROM Issue i WHERE i.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(i.createdAt) " +
           "ORDER BY DATE(i.createdAt)")
    List<Object[]> getIssueTrendByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目和日期统计Issue趋势
     */
    @Query("SELECT i.projectId, DATE(i.createdAt), COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.projectId, DATE(i.createdAt) " +
           "ORDER BY DATE(i.createdAt)")
    List<Object[]> getIssueTrendByProjectAndDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计Bug修复效率
     */
    @Query("SELECT AVG(i.resolutionTimeMinutes), MIN(i.resolutionTimeMinutes), MAX(i.resolutionTimeMinutes), " +
           "AVG(i.responseTimeMinutes) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.issueType = 'bug' AND i.status = 'closed' " +
           "AND i.createdAt BETWEEN :start AND :end")
    Object[] getBugFixEfficiencyStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按严重程度统计Bug修复时间
     */
    @Query("SELECT i.severity, COUNT(i), AVG(i.resolutionTimeMinutes), AVG(i.responseTimeMinutes) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.issueType = 'bug' AND i.status = 'closed' " +
           "AND i.severity IS NOT NULL AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.severity " +
           "ORDER BY AVG(i.resolutionTimeMinutes) ASC")
    List<Object[]> getBugFixTimesBySeverity(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按优先级统计Bug修复时间
     */
    @Query("SELECT i.priority, COUNT(i), AVG(i.resolutionTimeMinutes), AVG(i.responseTimeMinutes) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.issueType = 'bug' AND i.status = 'closed' " +
           "AND i.priority IS NOT NULL AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.priority " +
           "ORDER BY AVG(i.resolutionTimeMinutes) ASC")
    List<Object[]> getBugFixTimesByPriority(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找长时间未解决的Issue
     */
    @Query("SELECT i FROM Issue i WHERE i.status = 'opened' " +
           "AND TIMESTAMPDIFF(HOUR, i.createdAt, CURRENT_TIMESTAMP) > :hoursThreshold " +
           "ORDER BY i.createdAt ASC")
    List<Issue> findLongPendingIssues(@Param("hoursThreshold") Integer hoursThreshold);
    
    /**
     * 按项目查找长时间未解决的Issue
     */
    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.status = 'opened' " +
           "AND TIMESTAMPDIFF(HOUR, i.createdAt, CURRENT_TIMESTAMP) > :hoursThreshold " +
           "ORDER BY i.createdAt ASC")
    List<Issue> findLongPendingIssuesByProject(
        @Param("projectId") String projectId, @Param("hoursThreshold") Integer hoursThreshold);
    
    /**
     * 查找超过截止日期的Issue
     */
    @Query("SELECT i FROM Issue i WHERE i.dueDate IS NOT NULL AND i.dueDate < CURRENT_TIMESTAMP " +
           "AND i.status != 'closed' ORDER BY i.dueDate ASC")
    List<Issue> findOverdueIssues();
    
    /**
     * 按项目查找超过截止日期的Issue
     */
    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.dueDate IS NOT NULL " +
           "AND i.dueDate < CURRENT_TIMESTAMP AND i.status != 'closed' ORDER BY i.dueDate ASC")
    List<Issue> findOverdueIssuesByProject(@Param("projectId") String projectId);
    
    /**
     * 统计Issue类型分布
     */
    @Query("SELECT i.issueType, COUNT(i) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.issueType")
    List<Object[]> getIssueTypeStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计Issue状态分布
     */
    @Query("SELECT i.status, COUNT(i) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.status")
    List<Object[]> getIssueStatusStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计Issue优先级分布
     */
    @Query("SELECT i.priority, COUNT(i) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.priority IS NOT NULL " +
           "AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.priority")
    List<Object[]> getIssuePriorityStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最活跃的Issue创建者
     */
    @Query("SELECT i.authorId, i.authorName, COUNT(i) " +
           "FROM Issue i WHERE i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.authorId, i.authorName " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getMostActiveIssueCreators(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最高效的Issue解决者
     */
    @Query("SELECT i.assigneeId, i.assigneeName, COUNT(i), AVG(i.resolutionTimeMinutes) " +
           "FROM Issue i WHERE i.status = 'closed' AND i.assigneeId IS NOT NULL " +
           "AND i.resolutionTimeMinutes IS NOT NULL AND i.closedAt BETWEEN :start AND :end " +
           "GROUP BY i.assigneeId, i.assigneeName " +
           "HAVING COUNT(i) >= :minIssues " +
           "ORDER BY AVG(i.resolutionTimeMinutes) ASC")
    List<Object[]> getMostEfficientIssueResolvers(
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("minIssues") Integer minIssues);
    
    /**
     * 统计里程碑的Issue完成情况
     */
    @Query("SELECT i.milestoneTitle, COUNT(i), " +
           "COUNT(CASE WHEN i.status = 'closed' THEN 1 END) " +
           "FROM Issue i WHERE i.projectId = :projectId AND i.milestoneTitle IS NOT NULL " +
           "AND i.createdAt BETWEEN :start AND :end " +
           "GROUP BY i.milestoneTitle " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getMilestoneIssueStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 比较两个时间段的Issue处理效率
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN i.createdAt BETWEEN :period1Start AND :period1End THEN 1 END), " +
           "COUNT(CASE WHEN i.createdAt BETWEEN :period2Start AND :period2End THEN 1 END), " +
           "AVG(CASE WHEN i.createdAt BETWEEN :period1Start AND :period1End AND i.resolutionTimeMinutes IS NOT NULL " +
           "THEN i.resolutionTimeMinutes END), " +
           "AVG(CASE WHEN i.createdAt BETWEEN :period2Start AND :period2End AND i.resolutionTimeMinutes IS NOT NULL " +
           "THEN i.resolutionTimeMinutes END) " +
           "FROM Issue i WHERE i.projectId = :projectId " +
           "AND (i.createdAt BETWEEN :period1Start AND :period1End OR i.createdAt BETWEEN :period2Start AND :period2End)")
    Object[] compareIssueEfficiencyBetweenPeriods(
        @Param("projectId") String projectId,
        @Param("period1Start") LocalDateTime period1Start, @Param("period1End") LocalDateTime period1End,
        @Param("period2Start") LocalDateTime period2Start, @Param("period2End") LocalDateTime period2End);
}