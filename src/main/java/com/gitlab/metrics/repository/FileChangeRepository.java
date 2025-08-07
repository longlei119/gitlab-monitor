package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件变更Repository接口
 * 提供文件变更相关的数据访问方法，包括文件变更统计和分析查询
 */
@Repository
public interface FileChangeRepository extends JpaRepository<FileChange, Long> {
    
    /**
     * 根据提交ID查找文件变更记录
     */
    List<FileChange> findByCommitId(Long commitId);
    
    /**
     * 根据文件路径查找文件变更记录
     */
    List<FileChange> findByFilePathOrderByCommitTimestampDesc(String filePath);
    
    /**
     * 根据变更类型查找文件变更记录
     */
    List<FileChange> findByChangeTypeOrderByCommitTimestampDesc(String changeType);
    
    /**
     * 统计文件变更频率
     * 返回：文件路径, 变更次数, 总新增行数, 总删除行数
     */
    @Query("SELECT f.filePath, COUNT(f), " +
           "COALESCE(SUM(f.linesAdded), 0), COALESCE(SUM(f.linesDeleted), 0) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.filePath " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getFileChangeFrequency(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按项目统计文件变更频率
     */
    @Query("SELECT f.filePath, COUNT(f), " +
           "COALESCE(SUM(f.linesAdded), 0), COALESCE(SUM(f.linesDeleted), 0) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.filePath " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getFileChangeFrequencyByProject(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计变更类型分布
     */
    @Query("SELECT f.changeType, COUNT(f) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.changeType")
    List<Object[]> getChangeTypeStats(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最频繁修改的文件（热点文件）
     */
    @Query("SELECT f.filePath, COUNT(f) as changeCount " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.filePath " +
           "HAVING COUNT(f) > :threshold " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getHotspotFiles(
        @Param("projectId") String projectId, @Param("threshold") Integer threshold,
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按文件扩展名统计变更
     */
    @Query("SELECT SUBSTRING(f.filePath, LOCATE('.', f.filePath) + 1) as extension, " +
           "COUNT(f), COALESCE(SUM(f.linesAdded), 0), COALESCE(SUM(f.linesDeleted), 0) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "AND f.filePath LIKE '%.%' " +
           "GROUP BY SUBSTRING(f.filePath, LOCATE('.', f.filePath) + 1) " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getChangeStatsByFileExtension(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按目录统计变更
     */
    @Query("SELECT SUBSTRING(f.filePath, 1, LOCATE('/', f.filePath) - 1) as directory, " +
           "COUNT(f), COALESCE(SUM(f.linesAdded), 0), COALESCE(SUM(f.linesDeleted), 0) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "AND f.filePath LIKE '%/%' " +
           "GROUP BY SUBSTRING(f.filePath, 1, LOCATE('/', f.filePath) - 1) " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getChangeStatsByDirectory(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计开发者对特定文件的贡献
     */
    @Query("SELECT c.developerId, c.developerName, COUNT(f), " +
           "COALESCE(SUM(f.linesAdded), 0), COALESCE(SUM(f.linesDeleted), 0) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE f.filePath = :filePath AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY c.developerId, c.developerName " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> getDeveloperContributionToFile(
        @Param("filePath") String filePath, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找大型文件变更（单次变更行数较多）
     */
    @Query("SELECT f, (COALESCE(f.linesAdded, 0) + COALESCE(f.linesDeleted, 0)) as totalLines " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "AND (COALESCE(f.linesAdded, 0) + COALESCE(f.linesDeleted, 0)) > :threshold " +
           "ORDER BY (COALESCE(f.linesAdded, 0) + COALESCE(f.linesDeleted, 0)) DESC")
    List<Object[]> getLargeFileChanges(
        @Param("projectId") String projectId, @Param("threshold") Integer threshold,
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计文件重命名操作
     */
    @Query("SELECT f.oldPath, f.filePath, c.developerId, c.timestamp " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE f.changeType = 'renamed' AND c.projectId = :projectId " +
           "AND c.timestamp BETWEEN :start AND :end " +
           "ORDER BY c.timestamp DESC")
    List<Object[]> getFileRenameOperations(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计新增文件
     */
    @Query("SELECT f.filePath, c.developerId, c.developerName, c.timestamp " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE f.changeType = 'added' AND c.projectId = :projectId " +
           "AND c.timestamp BETWEEN :start AND :end " +
           "ORDER BY c.timestamp DESC")
    List<Object[]> getNewFiles(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计删除文件
     */
    @Query("SELECT f.filePath, c.developerId, c.developerName, c.timestamp " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE f.changeType = 'deleted' AND c.projectId = :projectId " +
           "AND c.timestamp BETWEEN :start AND :end " +
           "ORDER BY c.timestamp DESC")
    List<Object[]> getDeletedFiles(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计文件变更趋势
     */
    @Query("SELECT DATE(c.timestamp), COUNT(f), " +
           "COUNT(CASE WHEN f.changeType = 'added' THEN 1 END), " +
           "COUNT(CASE WHEN f.changeType = 'modified' THEN 1 END), " +
           "COUNT(CASE WHEN f.changeType = 'deleted' THEN 1 END) " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY DATE(c.timestamp) " +
           "ORDER BY DATE(c.timestamp)")
    List<Object[]> getFileChangeTrendByDate(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找从未修改的文件（在指定时间段内）
     */
    @Query("SELECT DISTINCT f1.filePath " +
           "FROM FileChange f1 JOIN f1.commit c1 " +
           "WHERE c1.projectId = :projectId AND c1.timestamp < :start " +
           "AND f1.filePath NOT IN (" +
           "  SELECT f2.filePath FROM FileChange f2 JOIN f2.commit c2 " +
           "  WHERE c2.projectId = :projectId AND c2.timestamp BETWEEN :start AND :end" +
           ")")
    List<String> getUnchangedFiles(
        @Param("projectId") String projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计文件变更的平均大小
     */
    @Query("SELECT f.filePath, COUNT(f), " +
           "AVG(COALESCE(f.linesAdded, 0) + COALESCE(f.linesDeleted, 0)) as avgChangeSize " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.filePath " +
           "HAVING COUNT(f) >= :minChanges " +
           "ORDER BY AVG(COALESCE(f.linesAdded, 0) + COALESCE(f.linesDeleted, 0)) DESC")
    List<Object[]> getAverageFileChangeSize(
        @Param("projectId") String projectId, @Param("minChanges") Integer minChanges,
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找协作最频繁的文件（多个开发者修改）
     */
    @Query("SELECT f.filePath, COUNT(DISTINCT c.developerId) as developerCount, COUNT(f) as changeCount " +
           "FROM FileChange f JOIN f.commit c " +
           "WHERE c.projectId = :projectId AND c.timestamp BETWEEN :start AND :end " +
           "GROUP BY f.filePath " +
           "HAVING COUNT(DISTINCT c.developerId) >= :minDevelopers " +
           "ORDER BY COUNT(DISTINCT c.developerId) DESC, COUNT(f) DESC")
    List<Object[]> getMostCollaborativeFiles(
        @Param("projectId") String projectId, @Param("minDevelopers") Integer minDevelopers,
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}