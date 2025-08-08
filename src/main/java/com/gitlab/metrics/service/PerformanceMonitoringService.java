package com.gitlab.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控服务
 * 监控数据库性能、查询执行时间、缓存命中率等关键指标
 */
@Service
public class PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 定期监控数据库性能（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void monitorDatabasePerformance() {
        try {
            logger.debug("开始数据库性能监控");
            
            // 监控慢查询
            monitorSlowQueries();
            
            // 监控表大小
            monitorTableSizes();
            
            // 监控索引使用情况
            monitorIndexUsage();
            
            // 监控连接池状态
            monitorConnectionPool();
            
            logger.debug("数据库性能监控完成");
            
        } catch (Exception e) {
            logger.error("数据库性能监控失败", e);
        }
    }
    
    /**
     * 监控慢查询
     */
    private void monitorSlowQueries() {
        try {
            String sql = "SELECT " +
                "sql_text, " +
                "exec_count, " +
                "avg_timer_wait/1000000000 as avg_time_seconds, " +
                "sum_timer_wait/1000000000 as total_time_seconds " +
                "FROM performance_schema.events_statements_summary_by_digest " +
                "WHERE avg_timer_wait > 1000000000 " +
                "ORDER BY avg_timer_wait DESC " +
                "LIMIT 10";
            
            List<Map<String, Object>> slowQueries = jdbcTemplate.queryForList(sql);
            
            if (!slowQueries.isEmpty()) {
                logger.warn("发现 {} 个慢查询", slowQueries.size());
                for (Map<String, Object> query : slowQueries) {
                    logger.warn("慢查询: 平均耗时 {}s, 执行次数 {}, SQL: {}", 
                              query.get("avg_time_seconds"), 
                              query.get("exec_count"),
                              truncateString(query.get("sql_text").toString(), 100));
                }
            }
            
        } catch (Exception e) {
            logger.debug("无法获取慢查询信息（可能是权限或版本问题）: {}", e.getMessage());
        }
    }
    
    /**
     * 监控表大小
     */
    private void monitorTableSizes() {
        try {
            String sql = "SELECT " +
                "table_name, " +
                "table_rows, " +
                "ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb, " +
                "ROUND((data_length / 1024 / 1024), 2) as data_mb, " +
                "ROUND((index_length / 1024 / 1024), 2) as index_mb " +
                "FROM information_schema.tables " +
                "WHERE table_schema = 'gitlab_metrics' " +
                "ORDER BY (data_length + index_length) DESC";
            
            List<Map<String, Object>> tableSizes = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> table : tableSizes) {
                String tableName = (String) table.get("table_name");
                Object sizeObj = table.get("size_mb");
                Object rowsObj = table.get("table_rows");
                
                if (sizeObj != null && rowsObj != null) {
                    double sizeMb = ((Number) sizeObj).doubleValue();
                    long rows = ((Number) rowsObj).longValue();
                    
                    if (sizeMb > 100) { // 表大小超过100MB时记录警告
                        logger.warn("大表警告: {} - 大小: {}MB, 行数: {}", tableName, sizeMb, rows);
                    } else {
                        logger.debug("表大小: {} - 大小: {}MB, 行数: {}", tableName, sizeMb, rows);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("无法获取表大小信息: {}", e.getMessage());
        }
    }
    
    /**
     * 监控索引使用情况
     */
    private void monitorIndexUsage() {
        try {
            String sql = "SELECT " +
                "table_name, " +
                "index_name, " +
                "cardinality, " +
                "CASE " +
                "    WHEN cardinality = 0 THEN 'UNUSED' " +
                "    WHEN cardinality < 100 THEN 'LOW_CARDINALITY' " +
                "    ELSE 'NORMAL' " +
                "END as status " +
                "FROM information_schema.statistics " +
                "WHERE table_schema = 'gitlab_metrics' " +
                "AND index_name != 'PRIMARY' " +
                "ORDER BY table_name, cardinality DESC";
            
            List<Map<String, Object>> indexStats = jdbcTemplate.queryForList(sql);
            
            int unusedIndexes = 0;
            int lowCardinalityIndexes = 0;
            
            for (Map<String, Object> index : indexStats) {
                String status = (String) index.get("status");
                if ("UNUSED".equals(status)) {
                    unusedIndexes++;
                    logger.warn("未使用的索引: {}.{}", index.get("table_name"), index.get("index_name"));
                } else if ("LOW_CARDINALITY".equals(status)) {
                    lowCardinalityIndexes++;
                    logger.debug("低基数索引: {}.{} (基数: {})", 
                               index.get("table_name"), index.get("index_name"), index.get("cardinality"));
                }
            }
            
            if (unusedIndexes > 0 || lowCardinalityIndexes > 0) {
                logger.info("索引分析: 未使用索引 {}, 低基数索引 {}", unusedIndexes, lowCardinalityIndexes);
            }
            
        } catch (Exception e) {
            logger.debug("无法获取索引使用情况: {}", e.getMessage());
        }
    }
    
    /**
     * 监控连接池状态
     */
    private void monitorConnectionPool() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            logger.debug("数据库连接信息: {} {}", 
                        metaData.getDatabaseProductName(), 
                        metaData.getDatabaseProductVersion());
            
            // 如果使用HikariCP，可以获取更详细的连接池信息
            if (dataSource.getClass().getName().contains("Hikari")) {
                // 这里可以添加HikariCP特定的监控逻辑
                logger.debug("使用HikariCP连接池");
            }
            
        } catch (Exception e) {
            logger.warn("无法获取连接池状态: {}", e.getMessage());
        }
    }
    
    /**
     * 获取数据库性能统计信息
     * 
     * @return 性能统计数据
     */
    public Map<String, Object> getDatabasePerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取查询统计
            String queryStatsSql = "SELECT " +
                "COUNT(*) as total_queries, " +
                "AVG(avg_timer_wait)/1000000000 as avg_query_time_seconds, " +
                "SUM(sum_timer_wait)/1000000000 as total_query_time_seconds " +
                "FROM performance_schema.events_statements_summary_by_digest";
            
            Map<String, Object> queryStats = jdbcTemplate.queryForMap(queryStatsSql);
            stats.put("query_stats", queryStats);
            
        } catch (Exception e) {
            logger.debug("无法获取查询统计: {}", e.getMessage());
        }
        
        try {
            // 获取表统计
            String tableStatsSql = "SELECT " +
                "COUNT(*) as total_tables, " +
                "SUM(table_rows) as total_rows, " +
                "ROUND(SUM((data_length + index_length) / 1024 / 1024), 2) as total_size_mb " +
                "FROM information_schema.tables " +
                "WHERE table_schema = 'gitlab_metrics'";
            
            Map<String, Object> tableStats = jdbcTemplate.queryForMap(tableStatsSql);
            stats.put("table_stats", tableStats);
            
        } catch (Exception e) {
            logger.debug("无法获取表统计: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 获取慢查询列表
     * 
     * @param limit 返回数量限制
     * @return 慢查询列表
     */
    public List<Map<String, Object>> getSlowQueries(int limit) {
        try {
            String sql = "SELECT " +
                "SUBSTRING(sql_text, 1, 200) as sql_text, " +
                "exec_count, " +
                "ROUND(avg_timer_wait/1000000000, 3) as avg_time_seconds, " +
                "ROUND(sum_timer_wait/1000000000, 3) as total_time_seconds, " +
                "ROUND(sum_rows_examined/exec_count, 0) as avg_rows_examined " +
                "FROM performance_schema.events_statements_summary_by_digest " +
                "WHERE avg_timer_wait > 1000000000 " +
                "ORDER BY avg_timer_wait DESC " +
                "LIMIT ?";
            
            return jdbcTemplate.queryForList(sql, limit);
            
        } catch (Exception e) {
            logger.debug("无法获取慢查询列表: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 获取表大小统计
     * 
     * @return 表大小统计
     */
    public List<Map<String, Object>> getTableSizeStats() {
        try {
            String sql = "SELECT " +
                "table_name, " +
                "table_rows, " +
                "ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb, " +
                "ROUND((data_length / 1024 / 1024), 2) as data_mb, " +
                "ROUND((index_length / 1024 / 1024), 2) as index_mb, " +
                "ROUND((index_length / data_length * 100), 2) as index_ratio_percent " +
                "FROM information_schema.tables " +
                "WHERE table_schema = 'gitlab_metrics' " +
                "AND table_type = 'BASE TABLE' " +
                "ORDER BY (data_length + index_length) DESC";
            
            return jdbcTemplate.queryForList(sql);
            
        } catch (Exception e) {
            logger.debug("无法获取表大小统计: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 执行数据库优化建议分析
     * 
     * @return 优化建议列表
     */
    public List<String> getOptimizationSuggestions() {
        List<String> suggestions = List.of();
        
        try {
            // 检查未使用的索引
            String unusedIndexSql = "SELECT CONCAT(table_name, '.', index_name) as unused_index " +
                "FROM information_schema.statistics " +
                "WHERE table_schema = 'gitlab_metrics' " +
                "AND index_name != 'PRIMARY' " +
                "AND cardinality = 0";
            
            List<String> unusedIndexes = jdbcTemplate.queryForList(unusedIndexSql, String.class);
            if (!unusedIndexes.isEmpty()) {
                suggestions.add("发现 " + unusedIndexes.size() + " 个未使用的索引，建议删除以节省空间");
            }
            
            // 检查大表
            String largeTableSql = "SELECT table_name " +
                "FROM information_schema.tables " +
                "WHERE table_schema = 'gitlab_metrics' " +
                "AND ((data_length + index_length) / 1024 / 1024) > 500";
            
            List<String> largeTables = jdbcTemplate.queryForList(largeTableSql, String.class);
            if (!largeTables.isEmpty()) {
                suggestions.add("发现 " + largeTables.size() + " 个大表（>500MB），建议考虑分区或归档");
            }
            
        } catch (Exception e) {
            logger.debug("无法生成优化建议: {}", e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}