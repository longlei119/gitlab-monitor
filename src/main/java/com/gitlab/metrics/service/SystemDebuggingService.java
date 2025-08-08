package com.gitlab.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统调试服务
 * 提供系统诊断、问题排查和性能分析功能
 */
@Service
public class SystemDebuggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemDebuggingService.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired(required = false)
    private MetricsEndpoint metricsEndpoint;
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;
    
    // 系统问题记录
    private final Map<String, SystemIssue> detectedIssues = new ConcurrentHashMap<>();
    
    /**
     * 执行系统健康检查
     * 
     * @return 系统健康状态报告
     */
    public SystemHealthReport performSystemHealthCheck() {
        logger.info("开始执行系统健康检查");
        
        SystemHealthReport report = new SystemHealthReport();
        report.setCheckTime(LocalDateTime.now());
        
        try {
            // 检查JVM状态
            report.setJvmHealth(checkJvmHealth());
            
            // 检查数据库连接
            report.setDatabaseHealth(checkDatabaseHealth());
            
            // 检查Redis连接
            report.setRedisHealth(checkRedisHealth());
            
            // 检查缓存状态
            report.setCacheHealth(checkCacheHealth());
            
            // 检查线程状态
            report.setThreadHealth(checkThreadHealth());
            
            // 检查内存使用
            report.setMemoryHealth(checkMemoryHealth());
            
            // 检查磁盘空间
            report.setDiskHealth(checkDiskHealth());
            
            // 分析系统问题
            report.setDetectedIssues(analyzeSystemIssues());
            
            // 生成建议
            report.setRecommendations(generateRecommendations(report));
            
            logger.info("系统健康检查完成，总体状态: {}", report.getOverallHealth());
            
        } catch (Exception e) {
            logger.error("系统健康检查失败", e);
            report.setOverallHealth(HealthStatus.CRITICAL);
            report.addIssue("HEALTH_CHECK_FAILED", "系统健康检查执行失败: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * 检查JVM健康状态
     */
    private HealthStatus checkJvmHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            if (heapUsagePercent > 90) {
                detectedIssues.put("HIGH_HEAP_USAGE", new SystemIssue(
                    "HIGH_HEAP_USAGE", 
                    "堆内存使用率过高: " + String.format("%.2f%%", heapUsagePercent),
                    IssueSeverity.CRITICAL
                ));
                return HealthStatus.CRITICAL;
            } else if (heapUsagePercent > 80) {
                detectedIssues.put("MODERATE_HEAP_USAGE", new SystemIssue(
                    "MODERATE_HEAP_USAGE", 
                    "堆内存使用率较高: " + String.format("%.2f%%", heapUsagePercent),
                    IssueSeverity.WARNING
                ));
                return HealthStatus.WARNING;
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查JVM健康状态失败", e);
            return HealthStatus.UNKNOWN;
        }
    }
    
    /**
     * 检查数据库健康状态
     */
    private HealthStatus checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 检查连接是否有效
            if (!connection.isValid(5)) {
                detectedIssues.put("DATABASE_CONNECTION_INVALID", new SystemIssue(
                    "DATABASE_CONNECTION_INVALID", 
                    "数据库连接无效",
                    IssueSeverity.CRITICAL
                ));
                return HealthStatus.CRITICAL;
            }
            
            // 检查数据库版本
            String dbVersion = metaData.getDatabaseProductVersion();
            logger.debug("数据库版本: {}", dbVersion);
            
            // 检查慢查询
            List<Map<String, Object>> slowQueries = performanceMonitoringService.getSlowQueries(5);
            if (slowQueries.size() > 3) {
                detectedIssues.put("TOO_MANY_SLOW_QUERIES", new SystemIssue(
                    "TOO_MANY_SLOW_QUERIES", 
                    "发现过多慢查询: " + slowQueries.size() + " 个",
                    IssueSeverity.WARNING
                ));
                return HealthStatus.WARNING;
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查数据库健康状态失败", e);
            detectedIssues.put("DATABASE_CHECK_FAILED", new SystemIssue(
                "DATABASE_CHECK_FAILED", 
                "数据库健康检查失败: " + e.getMessage(),
                IssueSeverity.CRITICAL
            ));
            return HealthStatus.CRITICAL;
        }
    }
    
    /**
     * 检查Redis健康状态
     */
    private HealthStatus checkRedisHealth() {
        try {
            // 执行ping命令
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if (!"PONG".equals(pong)) {
                detectedIssues.put("REDIS_PING_FAILED", new SystemIssue(
                    "REDIS_PING_FAILED", 
                    "Redis ping命令失败",
                    IssueSeverity.CRITICAL
                ));
                return HealthStatus.CRITICAL;
            }
            
            // 检查内存使用
            Properties info = redisTemplate.getConnectionFactory().getConnection().info("memory");
            String usedMemory = info.getProperty("used_memory");
            String maxMemory = info.getProperty("maxmemory");
            
            if (usedMemory != null && maxMemory != null && !maxMemory.equals("0")) {
                long used = Long.parseLong(usedMemory);
                long max = Long.parseLong(maxMemory);
                double usagePercent = (double) used / max * 100;
                
                if (usagePercent > 90) {
                    detectedIssues.put("REDIS_HIGH_MEMORY", new SystemIssue(
                        "REDIS_HIGH_MEMORY", 
                        "Redis内存使用率过高: " + String.format("%.2f%%", usagePercent),
                        IssueSeverity.WARNING
                    ));
                    return HealthStatus.WARNING;
                }
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查Redis健康状态失败", e);
            detectedIssues.put("REDIS_CHECK_FAILED", new SystemIssue(
                "REDIS_CHECK_FAILED", 
                "Redis健康检查失败: " + e.getMessage(),
                IssueSeverity.CRITICAL
            ));
            return HealthStatus.CRITICAL;
        }
    }
    
    /**
     * 检查缓存健康状态
     */
    private HealthStatus checkCacheHealth() {
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            
            for (String cacheName : cacheNames) {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache == null) {
                    detectedIssues.put("CACHE_NOT_AVAILABLE", new SystemIssue(
                        "CACHE_NOT_AVAILABLE", 
                        "缓存不可用: " + cacheName,
                        IssueSeverity.WARNING
                    ));
                    return HealthStatus.WARNING;
                }
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查缓存健康状态失败", e);
            return HealthStatus.WARNING;
        }
    }
    
    /**
     * 检查线程健康状态
     */
    private HealthStatus checkThreadHealth() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            
            // 检查线程数量是否过多
            if (threadCount > 500) {
                detectedIssues.put("TOO_MANY_THREADS", new SystemIssue(
                    "TOO_MANY_THREADS", 
                    "线程数量过多: " + threadCount,
                    IssueSeverity.WARNING
                ));
                return HealthStatus.WARNING;
            }
            
            // 检查死锁
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                detectedIssues.put("THREAD_DEADLOCK", new SystemIssue(
                    "THREAD_DEADLOCK", 
                    "检测到线程死锁: " + deadlockedThreads.length + " 个线程",
                    IssueSeverity.CRITICAL
                ));
                return HealthStatus.CRITICAL;
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查线程健康状态失败", e);
            return HealthStatus.WARNING;
        }
    }
    
    /**
     * 检查内存健康状态
     */
    private HealthStatus checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 检查堆内存
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            // 检查非堆内存
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            if (heapUsagePercent > 85) {
                detectedIssues.put("HIGH_MEMORY_USAGE", new SystemIssue(
                    "HIGH_MEMORY_USAGE", 
                    "内存使用率过高: " + String.format("%.2f%%", heapUsagePercent),
                    IssueSeverity.WARNING
                ));
                return HealthStatus.WARNING;
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查内存健康状态失败", e);
            return HealthStatus.WARNING;
        }
    }
    
    /**
     * 检查磁盘健康状态
     */
    private HealthStatus checkDiskHealth() {
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            double usagePercent = (double) (totalSpace - freeSpace) / totalSpace * 100;
            
            if (usagePercent > 90) {
                detectedIssues.put("LOW_DISK_SPACE", new SystemIssue(
                    "LOW_DISK_SPACE", 
                    "磁盘空间不足: " + String.format("%.2f%%", usagePercent) + " 已使用",
                    IssueSeverity.CRITICAL
                ));
                return HealthStatus.CRITICAL;
            } else if (usagePercent > 80) {
                detectedIssues.put("MODERATE_DISK_USAGE", new SystemIssue(
                    "MODERATE_DISK_USAGE", 
                    "磁盘空间使用率较高: " + String.format("%.2f%%", usagePercent),
                    IssueSeverity.WARNING
                ));
                return HealthStatus.WARNING;
            }
            
            return HealthStatus.HEALTHY;
            
        } catch (Exception e) {
            logger.error("检查磁盘健康状态失败", e);
            return HealthStatus.WARNING;
        }
    }
    
    /**
     * 分析系统问题
     */
    private List<SystemIssue> analyzeSystemIssues() {
        return new ArrayList<>(detectedIssues.values());
    }
    
    /**
     * 生成建议
     */
    private List<String> generateRecommendations(SystemHealthReport report) {
        List<String> recommendations = new ArrayList<>();
        
        if (report.getJvmHealth() == HealthStatus.WARNING || report.getJvmHealth() == HealthStatus.CRITICAL) {
            recommendations.add("建议增加JVM堆内存大小或优化内存使用");
        }
        
        if (report.getDatabaseHealth() == HealthStatus.WARNING) {
            recommendations.add("建议优化数据库查询或增加数据库连接池大小");
        }
        
        if (report.getRedisHealth() == HealthStatus.WARNING) {
            recommendations.add("建议检查Redis配置或增加Redis内存");
        }
        
        if (report.getThreadHealth() == HealthStatus.WARNING) {
            recommendations.add("建议检查线程池配置或优化异步处理逻辑");
        }
        
        if (report.getDiskHealth() == HealthStatus.WARNING || report.getDiskHealth() == HealthStatus.CRITICAL) {
            recommendations.add("建议清理磁盘空间或增加存储容量");
        }
        
        return recommendations;
    }
    
    /**
     * 获取系统性能指标
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // JVM指标
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            metrics.put("heap_used", memoryBean.getHeapMemoryUsage().getUsed());
            metrics.put("heap_max", memoryBean.getHeapMemoryUsage().getMax());
            metrics.put("non_heap_used", memoryBean.getNonHeapMemoryUsage().getUsed());
            
            // 线程指标
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            metrics.put("thread_count", threadBean.getThreadCount());
            metrics.put("daemon_thread_count", threadBean.getDaemonThreadCount());
            
            // 操作系统指标
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            metrics.put("cpu_load", osBean.getProcessCpuLoad());
            metrics.put("available_processors", osBean.getAvailableProcessors());
            
            // 数据库性能指标
            Map<String, Object> dbStats = performanceMonitoringService.getDatabasePerformanceStats();
            metrics.put("database_stats", dbStats);
            
        } catch (Exception e) {
            logger.error("获取系统指标失败", e);
        }
        
        return metrics;
    }
    
    /**
     * 清理检测到的问题
     */
    public void clearDetectedIssues() {
        detectedIssues.clear();
        logger.info("已清理检测到的系统问题");
    }
    
    // 内部类定义
    
    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL, UNKNOWN
    }
    
    public enum IssueSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public static class SystemIssue {
        private String code;
        private String description;
        private IssueSeverity severity;
        private LocalDateTime detectedAt;
        
        public SystemIssue(String code, String description, IssueSeverity severity) {
            this.code = code;
            this.description = description;
            this.severity = severity;
            this.detectedAt = LocalDateTime.now();
        }
        
        // Getters
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public IssueSeverity getSeverity() { return severity; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }
    
    public static class SystemHealthReport {
        private LocalDateTime checkTime;
        private HealthStatus overallHealth = HealthStatus.HEALTHY;
        private HealthStatus jvmHealth;
        private HealthStatus databaseHealth;
        private HealthStatus redisHealth;
        private HealthStatus cacheHealth;
        private HealthStatus threadHealth;
        private HealthStatus memoryHealth;
        private HealthStatus diskHealth;
        private List<SystemIssue> detectedIssues = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        
        public void addIssue(String code, String description) {
            detectedIssues.add(new SystemIssue(code, description, IssueSeverity.CRITICAL));
            overallHealth = HealthStatus.CRITICAL;
        }
        
        // Getters and Setters
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        public HealthStatus getOverallHealth() { return overallHealth; }
        public void setOverallHealth(HealthStatus overallHealth) { this.overallHealth = overallHealth; }
        
        public HealthStatus getJvmHealth() { return jvmHealth; }
        public void setJvmHealth(HealthStatus jvmHealth) { this.jvmHealth = jvmHealth; }
        
        public HealthStatus getDatabaseHealth() { return databaseHealth; }
        public void setDatabaseHealth(HealthStatus databaseHealth) { this.databaseHealth = databaseHealth; }
        
        public HealthStatus getRedisHealth() { return redisHealth; }
        public void setRedisHealth(HealthStatus redisHealth) { this.redisHealth = redisHealth; }
        
        public HealthStatus getCacheHealth() { return cacheHealth; }
        public void setCacheHealth(HealthStatus cacheHealth) { this.cacheHealth = cacheHealth; }
        
        public HealthStatus getThreadHealth() { return threadHealth; }
        public void setThreadHealth(HealthStatus threadHealth) { this.threadHealth = threadHealth; }
        
        public HealthStatus getMemoryHealth() { return memoryHealth; }
        public void setMemoryHealth(HealthStatus memoryHealth) { this.memoryHealth = memoryHealth; }
        
        public HealthStatus getDiskHealth() { return diskHealth; }
        public void setDiskHealth(HealthStatus diskHealth) { this.diskHealth = diskHealth; }
        
        public List<SystemIssue> getDetectedIssues() { return detectedIssues; }
        public void setDetectedIssues(List<SystemIssue> detectedIssues) { this.detectedIssues = detectedIssues; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}