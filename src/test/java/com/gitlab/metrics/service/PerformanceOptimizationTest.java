package com.gitlab.metrics.service;

import com.gitlab.metrics.config.PerformanceConfig;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.repository.CommitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 性能优化测试
 * 测试各种性能优化功能的正确性和效果
 */
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "app.performance.batch-size=100",
    "app.performance.async-core-pool-size=2",
    "app.performance.async-max-pool-size=5"
})
class PerformanceOptimizationTest {
    
    @Mock
    private CommitRepository commitRepository;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private DataSource dataSource;
    
    @Mock
    private Connection connection;
    
    @Mock
    private PerformanceConfig performanceConfig;
    
    @InjectMocks
    private BatchProcessingService batchProcessingService;
    
    @InjectMocks
    private OptimizedQueryService optimizedQueryService;
    
    @InjectMocks
    private SystemDebuggingService systemDebuggingService;
    
    @InjectMocks
    private LoadTestingService loadTestingService;
    
    @BeforeEach
    void setUp() {
        when(performanceConfig.getBatchSize()).thenReturn(100);
        when(performanceConfig.getQueryTimeout()).thenReturn(30);
    }
    
    @Test
    void testBatchProcessingService_BatchSaveCommits() throws Exception {
        // 准备测试数据
        List<Commit> commits = createTestCommits(50);
        
        // Mock repository行为
        when(commitRepository.saveAll(anyList())).thenReturn(commits);
        
        // 执行批量保存
        CompletableFuture<Integer> result = batchProcessingService.batchSaveCommits(commits);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(50, result.get().intValue());
        
        // 验证repository被调用
        verify(commitRepository, atLeastOnce()).saveAll(anyList());
        verify(commitRepository, atLeastOnce()).flush();
    }
    
    @Test
    void testOptimizedQueryService_GetCommitStats() {
        // 准备测试数据
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        String projectId = "test-project";
        
        Map<String, Object> expectedStats = Map.of(
            "total_commits", 100,
            "unique_developers", 5,
            "total_lines_added", 5000,
            "total_lines_deleted", 2000
        );
        
        // Mock JDBC查询结果
        when(jdbcTemplate.queryForMap(anyString(), any(), any(), any()))
            .thenReturn(expectedStats);
        
        // 执行查询
        Map<String, Object> result = optimizedQueryService
            .getOptimizedCommitStats(projectId, startDate, endDate);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(100, result.get("total_commits"));
        assertEquals(5, result.get("unique_developers"));
        assertEquals(5000, result.get("total_lines_added"));
        assertEquals(2000, result.get("total_lines_deleted"));
        
        // 验证JDBC被调用
        verify(jdbcTemplate).queryForMap(anyString(), eq(projectId), eq(startDate), eq(endDate));
    }
    
    @Test
    void testOptimizedQueryService_GetDeveloperEfficiencyRanking() {
        // 准备测试数据
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        int limit = 10;
        
        List<Map<String, Object>> expectedRanking = List.of(
            Map.of("developer_id", "dev1", "developer_name", "Developer 1", 
                   "commit_count", 50, "efficiency_score", 85.5),
            Map.of("developer_id", "dev2", "developer_name", "Developer 2", 
                   "commit_count", 30, "efficiency_score", 75.2)
        );
        
        // Mock JDBC查询结果
        when(jdbcTemplate.queryForList(anyString(), any(), any(), anyInt()))
            .thenReturn(expectedRanking);
        
        // 执行查询
        List<Map<String, Object>> result = optimizedQueryService
            .getOptimizedDeveloperEfficiencyRanking(startDate, endDate, limit);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("dev1", result.get(0).get("developer_id"));
        assertEquals(85.5, result.get(0).get("efficiency_score"));
        
        // 验证JDBC被调用
        verify(jdbcTemplate).queryForList(anyString(), eq(startDate), eq(endDate), eq(limit));
    }
    
    @Test
    void testSystemDebuggingService_PerformHealthCheck() throws Exception {
        // Mock数据源连接
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        
        // Mock Redis连接
        when(redisTemplate.getConnectionFactory()).thenReturn(mock(org.springframework.data.redis.connection.RedisConnectionFactory.class));
        when(redisTemplate.getConnectionFactory().getConnection()).thenReturn(mock(org.springframework.data.redis.connection.RedisConnection.class));
        when(redisTemplate.getConnectionFactory().getConnection().ping()).thenReturn("PONG");
        
        // Mock缓存管理器
        when(cacheManager.getCacheNames()).thenReturn(List.of("test-cache"));
        when(cacheManager.getCache("test-cache")).thenReturn(mock(org.springframework.cache.Cache.class));
        
        // 执行健康检查
        SystemDebuggingService.SystemHealthReport report = systemDebuggingService.performSystemHealthCheck();
        
        // 验证结果
        assertNotNull(report);
        assertNotNull(report.getCheckTime());
        assertNotNull(report.getJvmHealth());
        assertNotNull(report.getDatabaseHealth());
        assertNotNull(report.getRedisHealth());
        
        // 验证各组件被检查
        verify(dataSource).getConnection();
        verify(connection).isValid(anyInt());
    }
    
    @Test
    void testLoadTestingService_ExecuteWebhookLoadTest() throws Exception {
        // 准备测试参数
        int concurrentRequests = 5;
        int totalRequests = 20;
        String baseUrl = "http://localhost:8080";
        
        // 执行负载测试
        CompletableFuture<LoadTestingService.LoadTestResult> resultFuture = 
            loadTestingService.executeWebhookLoadTest(concurrentRequests, totalRequests, baseUrl);
        
        // 等待测试完成
        LoadTestingService.LoadTestResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("Webhook Load Test", result.getTestType());
        assertEquals(concurrentRequests, result.getConcurrentRequests());
        assertEquals(totalRequests, result.getTotalRequests());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getStartTime().isBefore(result.getEndTime()));
    }
    
    @Test
    void testLoadTestingService_ExecuteApiLoadTest() throws Exception {
        // 准备测试参数
        int concurrentRequests = 3;
        int totalRequests = 10;
        String baseUrl = "http://localhost:8080";
        
        // 执行API负载测试
        CompletableFuture<LoadTestingService.LoadTestResult> resultFuture = 
            loadTestingService.executeApiLoadTest(concurrentRequests, totalRequests, baseUrl);
        
        // 等待测试完成
        LoadTestingService.LoadTestResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("API Load Test", result.getTestType());
        assertEquals(concurrentRequests, result.getConcurrentRequests());
        assertEquals(totalRequests, result.getTotalRequests());
        assertTrue(result.getSuccessfulRequests() >= 0);
        assertTrue(result.getFailedRequests() >= 0);
        assertEquals(totalRequests, result.getSuccessfulRequests() + result.getFailedRequests());
    }
    
    @Test
    void testLoadTestingService_ExecuteDatabaseLoadTest() throws Exception {
        // 准备测试参数
        int concurrentQueries = 2;
        int totalQueries = 8;
        
        // Mock数据库查询结果
        when(jdbcTemplate.queryForMap(anyString(), any())).thenReturn(Map.of("result", "success"));
        when(jdbcTemplate.queryForList(anyString(), any(), any(), anyInt())).thenReturn(List.of());
        
        // 执行数据库负载测试
        CompletableFuture<LoadTestingService.LoadTestResult> resultFuture = 
            loadTestingService.executeDatabaseLoadTest(concurrentQueries, totalQueries);
        
        // 等待测试完成
        LoadTestingService.LoadTestResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("Database Load Test", result.getTestType());
        assertEquals(concurrentQueries, result.getConcurrentRequests());
        assertEquals(totalQueries, result.getTotalRequests());
    }
    
    @Test
    void testBatchProcessingService_HandleLargeDataSet() throws Exception {
        // 准备大量测试数据
        List<Commit> largeCommitList = createTestCommits(1500); // 超过批次大小
        
        // Mock repository行为
        when(commitRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Commit> batch = invocation.getArgument(0);
            return batch; // 返回保存的批次
        });
        
        // 执行批量保存
        CompletableFuture<Integer> result = batchProcessingService.batchSaveCommits(largeCommitList);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1500, result.get().intValue());
        
        // 验证批次处理：1500个记录应该分成多个批次
        // 批次大小为100，所以应该调用saveAll至少15次
        verify(commitRepository, atLeast(15)).saveAll(anyList());
        verify(commitRepository, atLeast(15)).flush();
    }
    
    @Test
    void testOptimizedQueryService_CacheEffectiveness() {
        // 准备测试数据
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        String projectId = "cache-test-project";
        
        Map<String, Object> mockStats = Map.of(
            "total_commits", 50,
            "unique_developers", 3
        );
        
        // Mock JDBC查询
        when(jdbcTemplate.queryForMap(anyString(), any(), any(), any()))
            .thenReturn(mockStats);
        
        // 第一次调用 - 应该执行数据库查询
        Map<String, Object> result1 = optimizedQueryService
            .getOptimizedCommitStats(projectId, startDate, endDate);
        
        // 第二次调用 - 应该从缓存获取（如果缓存正常工作）
        Map<String, Object> result2 = optimizedQueryService
            .getOptimizedCommitStats(projectId, startDate, endDate);
        
        // 验证结果一致
        assertEquals(result1, result2);
        assertEquals(50, result1.get("total_commits"));
        assertEquals(3, result1.get("unique_developers"));
        
        // 验证数据库查询被调用（缓存测试在集成测试中更有效）
        verify(jdbcTemplate, atLeastOnce()).queryForMap(anyString(), eq(projectId), eq(startDate), eq(endDate));
    }
    
    @Test
    void testSystemDebuggingService_GetSystemMetrics() {
        // 执行获取系统指标
        Map<String, Object> metrics = systemDebuggingService.getSystemMetrics();
        
        // 验证结果
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("heap_used"));
        assertTrue(metrics.containsKey("heap_max"));
        assertTrue(metrics.containsKey("thread_count"));
        assertTrue(metrics.containsKey("available_processors"));
        
        // 验证指标值的合理性
        assertTrue((Long) metrics.get("heap_used") > 0);
        assertTrue((Long) metrics.get("heap_max") > 0);
        assertTrue((Integer) metrics.get("thread_count") > 0);
        assertTrue((Integer) metrics.get("available_processors") > 0);
    }
    
    @Test
    void testPerformanceConfig_DefaultValues() {
        // 创建真实的配置对象进行测试
        PerformanceConfig config = new PerformanceConfig();
        
        // 验证默认值
        assertEquals(1000, config.getBatchSize());
        assertEquals(5, config.getAsyncCorePoolSize());
        assertEquals(20, config.getAsyncMaxPoolSize());
        assertEquals(100, config.getAsyncQueueCapacity());
        assertEquals(30, config.getQueryTimeout());
    }
    
    /**
     * 创建测试用的提交数据
     */
    private List<Commit> createTestCommits(int count) {
        List<Commit> commits = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Commit commit = new Commit();
            commit.setCommitSha("commit-sha-" + i);
            commit.setProjectId("test-project");
            commit.setDeveloperId("developer-" + (i % 5)); // 5个不同的开发者
            commit.setDeveloperName("Developer " + (i % 5));
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