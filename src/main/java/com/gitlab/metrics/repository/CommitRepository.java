package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码提交Repository接口
 * 提供代码提交相关的数据访问方法，包括统计查询和复杂分析
 */
@Repository
public interface CommitRepository extends JpaRepository<Commit, Long> {
    
    /**
     * 根据提交SHA查找提交记录
     */
    Optional<Commit> findByCommitSha(String commitSha);
    
    /**
     * 根据项目ID查找提交记录
     */
    List<Commit> findByProjectIdOrderByTimestampDesc(String projectId);
    
    /**
     * 根据开发者ID查找提交记录
     */
    List<Commit> findByDeveloperIdOrderByTimestampDesc(String developerId);
    
    /**
     * 根据开发者ID和时间范围查找提交记录
     */
    List<Commit> findByDeveloperIdAndTimestampBetween(String developerId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据项目ID和时间范围查找提交记录
     */
    List<Commit> findByProjectIdAndTimestampBetween(String projectId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据项目ID、开发者ID和时间范围查找提交记录
     */
    List<Commit> findByProjectIdAndDeveloperIdAndTimestampBetween(
        String projectId, String developerId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据分支查找提交记录
     */
    List<Commit> findByBranchOrderByTimestampDesc(String branch);
    
    /**
     * 统计开发者在指定时间范围内的提交数据
     * 返回：开发者ID, 提交次数, 新增代码行数, 删除代码行数, 修改文件数
     */
    @Query("SELECT c.developerId, c.developerName, COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0), COALESCE(SUM(c.filesChanged), 0) " +
           "FROM Commit c WHERE c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.developerId, c.developerName " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getDeveloperCommitStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计项目在指定时间范围内的提交数据
     */
    @Query("SELECT c.projectId, COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0), COALESCE(SUM(c.filesChanged), 0) " +
           "FROM Commit c WHERE c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.projectId " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getProjectCommitStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定项目中开发者的提交数据
     */
    @Query("SELECT c.developerId, c.developerName, COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0), COALESCE(SUM(c.filesChanged), 0) " +
           "FROM Commit c WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.developerId, c.developerName " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getDeveloperCommitStatsByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计提交趋势
     */
    @Query("SELECT DATE(c.timestamp), COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0) " +
           "FROM Commit c WHERE c.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(c.timestamp) " +
           "ORDER BY DATE(c.timestamp)")
    List<Object[]> getCommitTrendByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目和日期统计提交趋势
     */
    @Query("SELECT c.projectId, DATE(c.timestamp), COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0) " +
           "FROM Commit c WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.projectId, DATE(c.timestamp) " +
           "ORDER BY DATE(c.timestamp)")
    List<Object[]> getCommitTrendByProjectAndDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计开发者的活跃度（按小时分布）
     */
    @Query("SELECT HOUR(c.timestamp), COUNT(c) " +
           "FROM Commit c WHERE c.developerId = :developerId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY HOUR(c.timestamp) " +
           "ORDER BY HOUR(c.timestamp)")
    List<Object[]> getDeveloperActivityByHour(
        @Param("developerId") String developerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计分支的提交活跃度
     */
    @Query("SELECT c.branch, COUNT(c), " +
           "COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0) " +
           "FROM Commit c WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.branch " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getBranchActivityStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找大型提交（超过指定行数变更的提交）
     */
    @Query("SELECT c FROM Commit c WHERE (COALESCE(c.linesAdded, 0) + COALESCE(c.linesDeleted, 0)) > :threshold " +
           "AND c.timestamp BETWEEN :start AND :end ORDER BY (COALESCE(c.linesAdded, 0) + COALESCE(c.linesDeleted, 0)) DESC")
    List<Commit> findLargeCommits(
        @Param("threshold") Integer threshold, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计开发者的平均提交大小
     */
    @Query("SELECT c.developerId, c.developerName, " +
           "AVG(COALESCE(c.linesAdded, 0) + COALESCE(c.linesDeleted, 0)), " +
           "AVG(COALESCE(c.filesChanged, 0)) " +
           "FROM Commit c WHERE c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.developerId, c.developerName " +
           "ORDER BY AVG(COALESCE(c.linesAdded, 0) + COALESCE(c.linesDeleted, 0)) DESC")
    List<Object[]> getDeveloperAverageCommitSize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计项目的总体代码变更量
     */
    @Query("SELECT COALESCE(SUM(c.linesAdded), 0), COALESCE(SUM(c.linesDeleted), 0), " +
           "COALESCE(SUM(c.filesChanged), 0), COUNT(c) " +
           "FROM Commit c WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end")
    Object[] getProjectTotalStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}