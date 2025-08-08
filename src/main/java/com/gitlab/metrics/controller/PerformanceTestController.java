package com.gitlab.metrics.controller;

import com.gitlab.metrics.service.LoadTestingService;
import com.gitlab.metrics.service.PerformanceMonitoringService;
import com.gitlab.metrics.service.SystemDebuggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 性能测试控制器
 * 提供负载测试、系统调试和性能监控的API接口
 */
@RestController
@RequestMapping("/api/performance")
@PreAuthorize("hasRole('ADMIN')")
public class PerformanceTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestController.class);
    
    @Autowired
    private LoadTestingService loadTestingService;
    
    @Autowired
    private SystemDebuggingService systemDebuggingService;
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;
    
    /**
     * 执行Webhook负载测试
     * 
     * @param concurrentRequests 并发请求数
     * @param totalRequests 总请求数
     * @param baseUrl 基础URL
     * @return 测试结果
     */
    @PostMapping("/load-test/webhook")
    public ResponseEntity<CompletableFuture<LoadTestingService.LoadTestResult>> executeWebhookLoadTest(
            @RequestParam(defaultValue = "10") int concurrentRequests,
            @RequestParam(defaultValue = "100") int totalRequests,
            @RequestParam(defaultValue = "http://localhost:8080") String baseUrl) {
        
        logger.info("启动Webhook负载测试: 并发数={}, 总请求数={}", concurrentRequests, totalRequests);
        
        CompletableFuture<LoadTestingService.LoadTestResult> result = 
            loadTestingService.executeWebhookLoadTest(concurrentRequests, totalRequests, baseUrl);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 执行API负载测试
     * 
     * @param concurrentRequests 并发请求数
     * @param totalRequests 总请求数
     * @param baseUrl 基础URL
     * @return 测试结果
     */
    @PostMapping("/load-test/api")
    public ResponseEntity<CompletableFuture<LoadTestingService.LoadTestResult>> executeApiLoadTest(
            @RequestParam(defaultValue = "10") int concurrentRequests,
            @RequestParam(defaultValue = "100") int totalRequests,
            @RequestParam(defaultValue = "http://localhost:8080") String baseUrl) {
        
        logger.info("启动API负载测试: 并发数={}, 总请求数={}", concurrentRequests, totalRequests);
        
        CompletableFuture<LoadTestingService.LoadTestResult> result = 
            loadTestingService.executeApiLoadTest(concurrentRequests, totalRequests, baseUrl);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 执行数据库负载测试
     * 
     * @param concurrentQueries 并发查询数
     * @param totalQueries 总查询数
     * @return 测试结果
     */
    @PostMapping("/load-test/database")
    public ResponseEntity<CompletableFuture<LoadTestingService.LoadTestResult>> executeDatabaseLoadTest(
            @RequestParam(defaultValue = "5") int concurrentQueries,
            @RequestParam(defaultValue = "50") int totalQueries) {
        
        logger.info("启动数据库负载测试: 并发数={}, 总查询数={}", concurrentQueries, totalQueries);
        
        CompletableFuture<LoadTestingService.LoadTestResult> result = 
            loadTestingService.executeDatabaseLoadTest(concurrentQueries, totalQueries);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 执行系统健康检查
     * 
     * @return 系统健康报告
     */
    @GetMapping("/health-check")
    public ResponseEntity<SystemDebuggingService.SystemHealthReport> performHealthCheck() {
        logger.info("执行系统健康检查");
        
        SystemDebuggingService.SystemHealthReport report = 
            systemDebuggingService.performSystemHealthCheck();
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * 获取系统性能指标
     * 
     * @return 系统性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        logger.debug("获取系统性能指标");
        
        Map<String, Object> metrics = systemDebuggingService.getSystemMetrics();
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取数据库性能统计
     * 
     * @return 数据库性能统计
     */
    @GetMapping("/database/stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        logger.debug("获取数据库性能统计");
        
        Map<String, Object> stats = performanceMonitoringService.getDatabasePerformanceStats();
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取慢查询列表
     * 
     * @param limit 返回数量限制
     * @return 慢查询列表
     */
    @GetMapping("/database/slow-queries")
    public ResponseEntity<List<Map<String, Object>>> getSlowQueries(
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.debug("获取慢查询列表，限制数量: {}", limit);
        
        List<Map<String, Object>> slowQueries = performanceMonitoringService.getSlowQueries(limit);
        
        return ResponseEntity.ok(slowQueries);
    }
    
    /**
     * 获取表大小统计
     * 
     * @return 表大小统计
     */
    @GetMapping("/database/table-sizes")
    public ResponseEntity<List<Map<String, Object>>> getTableSizes() {
        logger.debug("获取表大小统计");
        
        List<Map<String, Object>> tableSizes = performanceMonitoringService.getTableSizeStats();
        
        return ResponseEntity.ok(tableSizes);
    }
    
    /**
     * 获取优化建议
     * 
     * @return 优化建议列表
     */
    @GetMapping("/optimization/suggestions")
    public ResponseEntity<List<String>> getOptimizationSuggestions() {
        logger.debug("获取优化建议");
        
        List<String> suggestions = performanceMonitoringService.getOptimizationSuggestions();
        
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * 清理检测到的系统问题
     * 
     * @return 操作结果
     */
    @PostMapping("/debug/clear-issues")
    public ResponseEntity<String> clearDetectedIssues() {
        logger.info("清理检测到的系统问题");
        
        systemDebuggingService.clearDetectedIssues();
        
        return ResponseEntity.ok("系统问题已清理");
    }
    
    /**
     * 触发垃圾回收
     * 
     * @return 操作结果
     */
    @PostMapping("/debug/gc")
    public ResponseEntity<Map<String, Object>> triggerGarbageCollection() {
        logger.info("触发垃圾回收");
        
        // 记录GC前的内存状态
        Runtime runtime = Runtime.getRuntime();
        long beforeGC = runtime.totalMemory() - runtime.freeMemory();
        
        // 触发GC
        System.gc();
        
        // 等待一小段时间让GC完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 记录GC后的内存状态
        long afterGC = runtime.totalMemory() - runtime.freeMemory();
        long freedMemory = beforeGC - afterGC;
        
        Map<String, Object> result = Map.of(
            "before_gc_mb", beforeGC / 1024 / 1024,
            "after_gc_mb", afterGC / 1024 / 1024,
            "freed_memory_mb", freedMemory / 1024 / 1024,
            "total_memory_mb", runtime.totalMemory() / 1024 / 1024,
            "free_memory_mb", runtime.freeMemory() / 1024 / 1024
        );
        
        logger.info("垃圾回收完成，释放内存: {} MB", freedMemory / 1024 / 1024);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取线程转储信息
     * 
     * @return 线程转储信息
     */
    @GetMapping("/debug/thread-dump")
    public ResponseEntity<Map<String, Object>> getThreadDump() {
        logger.debug("获取线程转储信息");
        
        java.lang.management.ThreadMXBean threadBean = 
            java.lang.management.ManagementFactory.getThreadMXBean();
        
        java.lang.management.ThreadInfo[] threadInfos = 
            threadBean.dumpAllThreads(true, true);
        
        Map<String, Object> result = Map.of(
            "thread_count", threadInfos.length,
            "daemon_thread_count", threadBean.getDaemonThreadCount(),
            "peak_thread_count", threadBean.getPeakThreadCount(),
            "total_started_thread_count", threadBean.getTotalStartedThreadCount()
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取内存转储信息
     * 
     * @return 内存转储信息
     */
    @GetMapping("/debug/memory-dump")
    public ResponseEntity<Map<String, Object>> getMemoryDump() {
        logger.debug("获取内存转储信息");
        
        java.lang.management.MemoryMXBean memoryBean = 
            java.lang.management.ManagementFactory.getMemoryMXBean();
        
        java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        java.lang.management.MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        Map<String, Object> result = Map.of(
            "heap_memory", Map.of(
                "init_mb", heapUsage.getInit() / 1024 / 1024,
                "used_mb", heapUsage.getUsed() / 1024 / 1024,
                "committed_mb", heapUsage.getCommitted() / 1024 / 1024,
                "max_mb", heapUsage.getMax() / 1024 / 1024
            ),
            "non_heap_memory", Map.of(
                "init_mb", nonHeapUsage.getInit() / 1024 / 1024,
                "used_mb", nonHeapUsage.getUsed() / 1024 / 1024,
                "committed_mb", nonHeapUsage.getCommitted() / 1024 / 1024,
                "max_mb", nonHeapUsage.getMax() / 1024 / 1024
            ),
            "object_pending_finalization_count", memoryBean.getObjectPendingFinalizationCount()
        );
        
        return ResponseEntity.ok(result);
    }
}