package com.gitlab.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 优化查询服务
 * 提供高性能的数据库查询方法，使用原生SQL和优化策略
 */
@Service
@Transactional(readOnly = true)
public class OptimizedQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedQueryService.class);
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取项目提交统计（优化版本）
     * 使用原生SQL和索引优化
     */
    @Cacheable(value = "statistics", key = "#projectId + '_commits_' + #startDate + '_' + #endDate")
    public Map<String, Object> getOptimizedCommitStats(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("获取优化的提交统计: projectId={}, startDate={}, endDate={}", projectId, startDate, endDate);
        
        String sql = "SELECT " +
            "COUNT(*) as total_commits, " +
            "COUNT(DISTINCT developer_id) as unique_developers, " +
            "COALESCE(SUM(lines_added), 0) as total_lines_added, " +
            "COALESCE(SUM(lines_deleted), 0) as total_lines_deleted, " +
            "COALESCE(SUM(files_changed), 0) as total_files_changed, " +
            "AVG(lines_added + lines_deleted) as avg_commit_size, " +
            "MAX(lines_added + lines_deleted) as max_commit_size, " +
            "MIN(lines_added + lines_deleted) as min_commit_size " +
            "FROM commits " +
            "WHERE project_id = ? " +
            "AND timestamp BETWEEN ? AND ?";
        
        return jdbcTemplate.queryForMap(sql, projectId, startDate, endDate);
    }
    
    /**
     * 获取开发者效率排行（优化版本）
     */
    @Cacheable(value = "developer", key = "'efficiency_ranking_' + #startDate + '_' + #endDate + '_' + #limit")
    public List<Map<String, Object>> getOptimizedDeveloperEfficiencyRanking(
            LocalDateTime startDate, LocalDateTime endDate, int limit) {
        logger.debug("获取优化的开发者效率排行: startDate={}, endDate={}, limit={}", startDate, endDate, limit);
        
        String sql = "SELECT " +
            "developer_id, " +
            "developer_name, " +
            "COUNT(*) as commit_count, " +
            "COALESCE(SUM(lines_added), 0) as total_lines_added, " +
            "COALESCE(SUM(lines_deleted), 0) as total_lines_deleted, " +
            "COALESCE(SUM(files_changed), 0) as total_files_changed, " +
            "AVG(lines_added + lines_deleted) as avg_commit_size, " +
            "COUNT(DISTINCT DATE(timestamp)) as active_days, " +
            "(COUNT(*) * 0.4 + " +
            " (COALESCE(SUM(lines_added), 0) + COALESCE(SUM(lines_deleted), 0)) / 1000.0 * 0.3 + " +
            " COALESCE(SUM(files_changed), 0) / 100.0 * 0.3) as efficiency_score " +
            "FROM commits " +
            "WHERE timestamp BETWEEN ? AND ? " +
            "GROUP BY developer_id, developer_name " +
            "HAVING commit_count > 0 " +
            "ORDER BY efficiency_score DESC " +
            "LIMIT ?";
        
        return jdbcTemplate.queryForList(sql, startDate, endDate, limit);
    }
    
    /**
     * 获取质量趋势数据（优化版本）
     */
    @Cacheable(value = "trends", key = "#projectId + '_quality_' + #groupBy + '_' + #startDate + '_' + #endDate")
    public List<Map<String, Object>> getOptimizedQualityTrends(
            String projectId, LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        logger.debug("获取优化的质量趋势: projectId={}, groupBy={}", projectId, groupBy);
        
        String dateFormat;
        switch (groupBy.toLowerCase()) {
            case "week":
                dateFormat = "%Y-%u";
                break;
            case "month":
                dateFormat = "%Y-%m";
                break;
            default:
                dateFormat = "%Y-%m-%d";
        }
        
        String sql = String.format(
            "SELECT " +
            "DATE_FORMAT(timestamp, '%s') as time_period, " +
            "COUNT(*) as record_count, " +
            "AVG(COALESCE(code_complexity, 0)) as avg_complexity, " +
            "AVG(COALESCE(duplicate_rate, 0)) as avg_duplicate_rate, " +
            "AVG(COALESCE(maintainability_index, 0)) as avg_maintainability, " +
            "AVG(COALESCE(technical_debt, 0)) as avg_technical_debt, " +
            "SUM(COALESCE(bugs, 0)) as total_bugs, " +
            "SUM(COALESCE(vulnerabilities, 0)) as total_vulnerabilities, " +
            "SUM(COALESCE(code_smells, 0)) as total_code_smells " +
            "FROM quality_metrics " +
            "WHERE project_id = ? " +
            "AND timestamp BETWEEN ? AND ? " +
            "GROUP BY time_period " +
            "ORDER BY time_period", dateFormat);
        
        return jdbcTemplate.queryForList(sql, projectId, startDate, endDate);
    }
    
    /**
     * 获取测试覆盖率趋势（优化版本）
     */
    @Cacheable(value = "trends", key = "#projectId + '_coverage_' + #startDate + '_' + #endDate")
    public List<Map<String, Object>> getOptimizedCoverageTrends(
            String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("获取优化的覆盖率趋势: projectId={}", projectId);
        
        String sql = "SELECT " +
            "DATE(timestamp) as date, " +
            "AVG(COALESCE(line_coverage, 0)) as avg_line_coverage, " +
            "AVG(COALESCE(branch_coverage, 0)) as avg_branch_coverage, " +
            "AVG(COALESCE(function_coverage, 0)) as avg_function_coverage, " +
            "COUNT(*) as record_count, " +
            "MAX(line_coverage) as max_line_coverage, " +
            "MIN(line_coverage) as min_line_coverage " +
            "FROM test_coverage " +
            "WHERE project_id = ? " +
            "AND timestamp BETWEEN ? AND ? " +
            "GROUP BY DATE(timestamp) " +
            "ORDER BY date";
        
        return jdbcTemplate.queryForList(sql, projectId, startDate, endDate);
    }
    
    /**
     * 获取项目活跃度统计（优化版本）
     */
    @Cacheable(value = "project", key = "'activity_' + #startDate + '_' + #endDate + '_' + #limit")
    public List<Map<String, Object>> getOptimizedProjectActivity(
            LocalDateTime startDate, LocalDateTime endDate, int limit) {
        logger.debug("获取优化的项目活跃度统计");
        
        String sql = "SELECT " +
            "c.project_id, " +
            "COUNT(c.id) as total_commits, " +
            "COUNT(DISTINCT c.developer_id) as unique_developers, " +
            "COALESCE(SUM(c.lines_added), 0) + COALESCE(SUM(c.lines_deleted), 0) as total_changes, " +
            "COUNT(DISTINCT DATE(c.timestamp)) as active_days, " +
            "AVG(c.lines_added + c.lines_deleted) as avg_commit_size, " +
            "(COUNT(c.id) * 0.3 + " +
            " COUNT(DISTINCT c.developer_id) * 0.2 + " +
            " COUNT(DISTINCT DATE(c.timestamp)) * 0.5) as activity_score " +
            "FROM commits c " +
            "WHERE c.timestamp BETWEEN ? AND ? " +
            "GROUP BY c.project_id " +
            "HAVING total_commits > 0 " +
            "ORDER BY activity_score DESC " +
            "LIMIT ?";
        
        return jdbcTemplate.queryForList(sql, startDate, endDate, limit);
    }
    
    /**
     * 获取热点文件分析（优化版本）
     */
    @Cacheable(value = "statistics", key = "#projectId + '_hotfiles_' + #startDate + '_' + #endDate + '_' + #limit")
    public List<Map<String, Object>> getOptimizedHotFiles(
            String projectId, LocalDateTime startDate, LocalDateTime endDate, int limit) {
        logger.debug("获取优化的热点文件分析: projectId={}", projectId);
        
        String sql = "SELECT " +
            "fc.file_path, " +
            "COUNT(*) as change_count, " +
            "COUNT(DISTINCT c.developer_id) as unique_developers, " +
            "COALESCE(SUM(fc.lines_added), 0) as total_lines_added, " +
            "COALESCE(SUM(fc.lines_deleted), 0) as total_lines_deleted, " +
            "AVG(fc.lines_added + fc.lines_deleted) as avg_change_size, " +
            "MAX(c.timestamp) as last_modified " +
            "FROM file_changes fc " +
            "JOIN commits c ON fc.commit_id = c.id " +
            "WHERE c.project_id = ? " +
            "AND c.timestamp BETWEEN ? AND ? " +
            "GROUP BY fc.file_path " +
            "HAVING change_count > 1 " +
            "ORDER BY change_count DESC, unique_developers DESC " +
            "LIMIT ?";
        
        return jdbcTemplate.queryForList(sql, projectId, startDate, endDate, limit);
    }
    
    /**
     * 获取分页的提交记录（优化版本）
     */
    public List<Map<String, Object>> getOptimizedCommitsPaginated(
            String projectId, Long lastCommitId, int pageSize) {
        logger.debug("获取分页提交记录: projectId={}, lastCommitId={}, pageSize={}", 
                    projectId, lastCommitId, pageSize);
        
        String sql;
        Object[] params;
        
        if (lastCommitId != null) {
            sql = "SELECT " +
                "id, commit_sha, developer_name, timestamp, message, " +
                "lines_added, lines_deleted, files_changed " +
                "FROM commits " +
                "WHERE project_id = ? AND id > ? " +
                "ORDER BY id ASC " +
                "LIMIT ?";
            params = new Object[]{projectId, lastCommitId, pageSize};
        } else {
            sql = "SELECT " +
                "id, commit_sha, developer_name, timestamp, message, " +
                "lines_added, lines_deleted, files_changed " +
                "FROM commits " +
                "WHERE project_id = ? " +
                "ORDER BY id ASC " +
                "LIMIT ?";
            params = new Object[]{projectId, pageSize};
        }
        
        return jdbcTemplate.queryForList(sql, params);
    }
    
    /**
     * 执行复杂的聚合查询（优化版本）
     */
    @Cacheable(value = "reports", key = "#projectId + '_comprehensive_' + #startDate + '_' + #endDate")
    public Map<String, Object> getOptimizedComprehensiveStats(
            String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("获取优化的综合统计数据: projectId={}", projectId);
        
        // 使用简化的查询，避免复杂的CTE
        String sql = "SELECT " +
            "(SELECT COUNT(*) FROM commits WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as total_commits, " +
            "(SELECT COUNT(DISTINCT developer_id) FROM commits WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as unique_developers, " +
            "(SELECT COALESCE(SUM(lines_added), 0) FROM commits WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as total_lines_added, " +
            "(SELECT COALESCE(SUM(lines_deleted), 0) FROM commits WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as total_lines_deleted, " +
            "(SELECT COUNT(*) FROM quality_metrics WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as quality_records, " +
            "(SELECT AVG(COALESCE(maintainability_index, 0)) FROM quality_metrics WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as avg_maintainability, " +
            "(SELECT SUM(COALESCE(bugs, 0)) FROM quality_metrics WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as total_bugs, " +
            "(SELECT AVG(COALESCE(line_coverage, 0)) FROM test_coverage WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as avg_line_coverage, " +
            "(SELECT COUNT(*) FROM test_coverage WHERE project_id = ? AND timestamp BETWEEN ? AND ?) as coverage_records";
        
        return jdbcTemplate.queryForMap(sql, 
            projectId, startDate, endDate,  // total_commits
            projectId, startDate, endDate,  // unique_developers
            projectId, startDate, endDate,  // total_lines_added
            projectId, startDate, endDate,  // total_lines_deleted
            projectId, startDate, endDate,  // quality_records
            projectId, startDate, endDate,  // avg_maintainability
            projectId, startDate, endDate,  // total_bugs
            projectId, startDate, endDate,  // avg_line_coverage
            projectId, startDate, endDate   // coverage_records
        );
    }
    
    /**
     * 获取数据库查询执行计划（用于性能调优）
     */
    public List<Map<String, Object>> getQueryExecutionPlan(String sql) {
        logger.debug("获取查询执行计划: {}", sql);
        
        String explainSql = "EXPLAIN " + sql;
        return jdbcTemplate.queryForList(explainSql);
    }
    
    /**
     * 预加载常用数据到缓存
     */
    public void preloadCache(List<String> projectIds) {
        logger.info("开始预加载缓存，项目数量: {}", projectIds.size());
        
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30); // 最近30天的数据
        
        for (String projectId : projectIds) {
            try {
                // 预加载常用的统计数据
                getOptimizedCommitStats(projectId, startDate, endDate);
                getOptimizedQualityTrends(projectId, startDate, endDate, "day");
                getOptimizedCoverageTrends(projectId, startDate, endDate);
                
                logger.debug("预加载项目缓存完成: {}", projectId);
            } catch (Exception e) {
                logger.warn("预加载项目缓存失败: {}", projectId, e);
            }
        }
        
        logger.info("缓存预加载完成");
    }
}