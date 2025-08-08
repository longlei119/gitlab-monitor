package com.gitlab.metrics.controller;

import com.gitlab.metrics.service.BatchProcessingService;
import com.gitlab.metrics.service.OptimizedQueryService;
import com.gitlab.metrics.service.SystemDebuggingService;
import com.gitlab.metrics.entity.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 简化测试控制器
 * 用于测试性能优化功能
 */
@RestController
@RequestMapping("/api/test")
public class SimpleTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleTestController.class);
    
    @Autowired
    private OptimizedQueryService optimizedQueryService;
    
    @Autowired
    private BatchProcessingService batchProcessingService;
    
    @Autowired
    private SystemDebuggingService systemDebuggingService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "GitLab Metrics Backend is running");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试批量处理
     */
    @PostMapping("/batch-test")
    public ResponseEntity<Map<String, Object>> testBatchProcessing(@RequestParam(defaultValue = "10") int count) {
        logger.info("测试批量处理，数量: {}", count);
        
        try {
            // 创建测试数据
            List<Commit> testCommits = createTestCommits(count);
            
            // 执行批量处理
            CompletableFuture<Integer> result = batchProcessingService.batchSaveCommits(testCommits);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "批量处理已启动");
            response.put("count", count);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量处理测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "批量处理测试失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 测试优化查询
     */
    @GetMapping("/query-test")
    public ResponseEntity<Map<String, Object>> testOptimizedQuery() {
        logger.info("测试优化查询");
        
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(30);
            
            // 测试统计查询
            Map<String, Object> stats = optimizedQueryService.getOptimizedCommitStats("test-project", startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "优化查询测试完成");
            response.put("stats", stats);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("优化查询测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "优化查询测试失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 测试系统诊断
     */
    @GetMapping("/system-test")
    public ResponseEntity<Map<String, Object>> testSystemDiagnostics() {
        logger.info("测试系统诊断");
        
        try {
            // 获取系统指标
            Map<String, Object> metrics = systemDebuggingService.getSystemMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "系统诊断测试完成");
            response.put("metrics", metrics);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("系统诊断测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "系统诊断测试失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 获取性能统计
     */
    @GetMapping("/performance-stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        Map<String, Object> response = new HashMap<>();
        
        // JVM内存信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total_mb", runtime.totalMemory() / 1024 / 1024);
        memory.put("free_mb", runtime.freeMemory() / 1024 / 1024);
        memory.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        memory.put("max_mb", runtime.maxMemory() / 1024 / 1024);
        
        // 线程信息
        Map<String, Object> threads = new HashMap<>();
        threads.put("active_count", Thread.activeCount());
        
        response.put("status", "success");
        response.put("memory", memory);
        response.put("threads", threads);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建测试提交数据
     */
    private List<Commit> createTestCommits(int count) {
        List<Commit> commits = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Commit commit = new Commit();
            commit.setCommitSha("test-commit-" + i);
            commit.setProjectId("test-project");
            commit.setDeveloperId("test-developer-" + (i % 3));
            commit.setDeveloperName("Test Developer " + (i % 3));
            commit.setTimestamp(LocalDateTime.now().minusHours(i));
            commit.setMessage("Test commit " + i);
            commit.setBranch("main");
            commit.setLinesAdded(10 + i);
            commit.setLinesDeleted(5 + i);
            commit.setFilesChanged(2 + (i % 3));
            
            commits.add(commit);
        }
        
        return commits;
    }
}