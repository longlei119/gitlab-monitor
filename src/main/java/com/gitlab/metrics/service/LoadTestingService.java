package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.entity.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载测试服务
 * 用于系统性能测试和压力测试，验证系统在高并发下的稳定性
 */
@Service
public class LoadTestingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadTestingService.class);
    
    @Autowired
    private CommitAnalysisService commitAnalysisService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private OptimizedQueryService optimizedQueryService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    private final AtomicInteger testCounter = new AtomicInteger(0);
    
    /**
     * 执行Webhook负载测试
     * 
     * @param concurrentRequests 并发请求数
     * @param totalRequests 总请求数
     * @param baseUrl 基础URL
     * @return 测试结果
     */
    @Async("taskExecutor")
    public CompletableFuture<LoadTestResult> executeWebhookLoadTest(
            int concurrentRequests, int totalRequests, String baseUrl) {
        
        logger.info("开始Webhook负载测试: 并发数={}, 总请求数={}", concurrentRequests, totalRequests);
        
        LoadTestResult result = new LoadTestResult();
        result.setTestType("Webhook Load Test");
        result.setStartTime(LocalDateTime.now());
        result.setConcurrentRequests(concurrentRequests);
        result.setTotalRequests(totalRequests);
        
        List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalRequests; i++) {
            CompletableFuture<RequestResult> future = CompletableFuture.supplyAsync(() -> {
                return executeWebhookRequest(baseUrl);
            });
            futures.add(future);
            
            // 控制并发数
            if (futures.size() >= concurrentRequests) {
                // 等待一批请求完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // 收集结果
                for (CompletableFuture<RequestResult> f : futures) {
                    try {
                        RequestResult requestResult = f.get();
                        result.addRequestResult(requestResult);
                    } catch (Exception e) {
                        logger.error("获取请求结果失败", e);
                        result.incrementFailedRequests();
                    }
                }
                
                futures.clear();
            }
        }
        
        // 处理剩余的请求
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<RequestResult> f : futures) {
                try {
                    RequestResult requestResult = f.get();
                    result.addRequestResult(requestResult);
                } catch (Exception e) {
                    logger.error("获取请求结果失败", e);
                    result.incrementFailedRequests();
                }
            }
        }
        
        result.setEndTime(LocalDateTime.now());
        result.calculateStatistics();
        
        logger.info("Webhook负载测试完成: 成功率={}%, 平均响应时间={}ms", 
                   result.getSuccessRate(), result.getAverageResponseTime());
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 执行API负载测试
     * 
     * @param concurrentRequests 并发请求数
     * @param totalRequests 总请求数
     * @param baseUrl 基础URL
     * @return 测试结果
     */
    @Async("taskExecutor")
    public CompletableFuture<LoadTestResult> executeApiLoadTest(
            int concurrentRequests, int totalRequests, String baseUrl) {
        
        logger.info("开始API负载测试: 并发数={}, 总请求数={}", concurrentRequests, totalRequests);
        
        LoadTestResult result = new LoadTestResult();
        result.setTestType("API Load Test");
        result.setStartTime(LocalDateTime.now());
        result.setConcurrentRequests(concurrentRequests);
        result.setTotalRequests(totalRequests);
        
        List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalRequests; i++) {
            CompletableFuture<RequestResult> future = CompletableFuture.supplyAsync(() -> {
                return executeApiRequest(baseUrl);
            });
            futures.add(future);
            
            // 控制并发数
            if (futures.size() >= concurrentRequests) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                for (CompletableFuture<RequestResult> f : futures) {
                    try {
                        RequestResult requestResult = f.get();
                        result.addRequestResult(requestResult);
                    } catch (Exception e) {
                        logger.error("获取API请求结果失败", e);
                        result.incrementFailedRequests();
                    }
                }
                
                futures.clear();
            }
        }
        
        // 处理剩余的请求
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<RequestResult> f : futures) {
                try {
                    RequestResult requestResult = f.get();
                    result.addRequestResult(requestResult);
                } catch (Exception e) {
                    logger.error("获取API请求结果失败", e);
                    result.incrementFailedRequests();
                }
            }
        }
        
        result.setEndTime(LocalDateTime.now());
        result.calculateStatistics();
        
        logger.info("API负载测试完成: 成功率={}%, 平均响应时间={}ms", 
                   result.getSuccessRate(), result.getAverageResponseTime());
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 执行数据库负载测试
     * 
     * @param concurrentQueries 并发查询数
     * @param totalQueries 总查询数
     * @return 测试结果
     */
    @Async("taskExecutor")
    public CompletableFuture<LoadTestResult> executeDatabaseLoadTest(
            int concurrentQueries, int totalQueries) {
        
        logger.info("开始数据库负载测试: 并发数={}, 总查询数={}", concurrentQueries, totalQueries);
        
        LoadTestResult result = new LoadTestResult();
        result.setTestType("Database Load Test");
        result.setStartTime(LocalDateTime.now());
        result.setConcurrentRequests(concurrentQueries);
        result.setTotalRequests(totalQueries);
        
        List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalQueries; i++) {
            CompletableFuture<RequestResult> future = CompletableFuture.supplyAsync(() -> {
                return executeDatabaseQuery();
            });
            futures.add(future);
            
            if (futures.size() >= concurrentQueries) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                for (CompletableFuture<RequestResult> f : futures) {
                    try {
                        RequestResult requestResult = f.get();
                        result.addRequestResult(requestResult);
                    } catch (Exception e) {
                        logger.error("获取数据库查询结果失败", e);
                        result.incrementFailedRequests();
                    }
                }
                
                futures.clear();
            }
        }
        
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<RequestResult> f : futures) {
                try {
                    RequestResult requestResult = f.get();
                    result.addRequestResult(requestResult);
                } catch (Exception e) {
                    logger.error("获取数据库查询结果失败", e);
                    result.incrementFailedRequests();
                }
            }
        }
        
        result.setEndTime(LocalDateTime.now());
        result.calculateStatistics();
        
        logger.info("数据库负载测试完成: 成功率={}%, 平均响应时间={}ms", 
                   result.getSuccessRate(), result.getAverageResponseTime());
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 执行单个Webhook请求
     */
    private RequestResult executeWebhookRequest(String baseUrl) {
        RequestResult result = new RequestResult();
        result.setStartTime(System.currentTimeMillis());
        
        try {
            // 生成模拟的push事件数据
            PushEventRequest pushEvent = generateMockPushEvent();
            
            // 直接调用服务方法而不是HTTP请求（避免网络开销）
            int processedCommits = commitAnalysisService.processPushEvent(pushEvent);
            
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(true);
            result.setResponseSize(processedCommits);
            
        } catch (Exception e) {
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            logger.debug("Webhook请求失败", e);
        }
        
        return result;
    }
    
    /**
     * 执行单个API请求
     */
    private RequestResult executeApiRequest(String baseUrl) {
        RequestResult result = new RequestResult();
        result.setStartTime(System.currentTimeMillis());
        
        try {
            // 随机选择API端点进行测试
            String[] endpoints = {
                "/api/metrics/commits",
                "/api/metrics/quality",
                "/api/metrics/efficiency"
            };
            
            String endpoint = endpoints[random.nextInt(endpoints.length)];
            String url = baseUrl + endpoint + "?startDate=2024-01-01&endDate=2024-12-31";
            
            // 使用优化查询服务直接调用
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);
            
            switch (endpoint) {
                case "/api/metrics/commits":
                    optimizedQueryService.getOptimizedCommitStats("project-1", startDate, endDate);
                    break;
                case "/api/metrics/quality":
                    optimizedQueryService.getOptimizedQualityTrends("project-1", startDate, endDate, "day");
                    break;
                case "/api/metrics/efficiency":
                    optimizedQueryService.getOptimizedDeveloperEfficiencyRanking(startDate, endDate, 10);
                    break;
            }
            
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(true);
            
        } catch (Exception e) {
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            logger.debug("API请求失败", e);
        }
        
        return result;
    }
    
    /**
     * 执行单个数据库查询
     */
    private RequestResult executeDatabaseQuery() {
        RequestResult result = new RequestResult();
        result.setStartTime(System.currentTimeMillis());
        
        try {
            // 随机执行不同类型的查询
            int queryType = random.nextInt(4);
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();
            
            switch (queryType) {
                case 0:
                    optimizedQueryService.getOptimizedCommitStats("project-" + random.nextInt(10), startDate, endDate);
                    break;
                case 1:
                    optimizedQueryService.getOptimizedDeveloperEfficiencyRanking(startDate, endDate, 20);
                    break;
                case 2:
                    optimizedQueryService.getOptimizedQualityTrends("project-" + random.nextInt(10), startDate, endDate, "day");
                    break;
                case 3:
                    optimizedQueryService.getOptimizedProjectActivity(startDate, endDate, 10);
                    break;
            }
            
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(true);
            
        } catch (Exception e) {
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            logger.debug("数据库查询失败", e);
        }
        
        return result;
    }
    
    /**
     * 生成模拟的Push事件数据
     */
    private PushEventRequest generateMockPushEvent() {
        PushEventRequest pushEvent = new PushEventRequest();
        pushEvent.setProjectId(random.nextInt(10) + 1);
        pushEvent.setRef("refs/heads/main");
        pushEvent.setUserName("test-user-" + random.nextInt(100));
        pushEvent.setUserEmail("test" + random.nextInt(100) + "@example.com");
        
        // 生成模拟的提交数据
        List<PushEventRequest.CommitInfo> commits = new ArrayList<>();
        int commitCount = random.nextInt(5) + 1; // 1-5个提交
        
        for (int i = 0; i < commitCount; i++) {
            PushEventRequest.CommitInfo commit = new PushEventRequest.CommitInfo();
            commit.setId("commit-" + testCounter.incrementAndGet() + "-" + System.currentTimeMillis());
            commit.setMessage("Test commit " + i);
            commit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 设置作者信息
            PushEventRequest.CommitInfo.Author author = new PushEventRequest.CommitInfo.Author();
            author.setName("Test Author " + random.nextInt(50));
            author.setEmail("author" + random.nextInt(50) + "@example.com");
            commit.setAuthor(author);
            
            // 生成文件变更
            List<String> added = new ArrayList<>();
            List<String> modified = new ArrayList<>();
            List<String> removed = new ArrayList<>();
            
            int fileCount = random.nextInt(10) + 1;
            for (int j = 0; j < fileCount; j++) {
                String fileName = "src/main/java/Test" + random.nextInt(1000) + ".java";
                int changeType = random.nextInt(3);
                switch (changeType) {
                    case 0:
                        added.add(fileName);
                        break;
                    case 1:
                        modified.add(fileName);
                        break;
                    case 2:
                        removed.add(fileName);
                        break;
                }
            }
            
            commit.setAdded(added);
            commit.setModified(modified);
            commit.setRemoved(removed);
            
            commits.add(commit);
        }
        
        pushEvent.setCommits(commits);
        
        // 设置项目信息
        PushEventRequest.Project project = new PushEventRequest.Project();
        project.setName("Test Project " + pushEvent.getProjectId());
        project.setPathWithNamespace("test-group/test-project-" + pushEvent.getProjectId());
        pushEvent.setProject(project);
        
        return pushEvent;
    }
    
    /**
     * 负载测试结果类
     */
    public static class LoadTestResult {
        private String testType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int concurrentRequests;
        private int totalRequests;
        private int successfulRequests = 0;
        private int failedRequests = 0;
        private long totalResponseTime = 0;
        private long minResponseTime = Long.MAX_VALUE;
        private long maxResponseTime = 0;
        private double averageResponseTime = 0;
        private double successRate = 0;
        private List<RequestResult> requestResults = new ArrayList<>();
        
        public void addRequestResult(RequestResult result) {
            requestResults.add(result);
            if (result.isSuccess()) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
            
            long responseTime = result.getResponseTime();
            totalResponseTime += responseTime;
            minResponseTime = Math.min(minResponseTime, responseTime);
            maxResponseTime = Math.max(maxResponseTime, responseTime);
        }
        
        public void incrementFailedRequests() {
            failedRequests++;
        }
        
        public void calculateStatistics() {
            if (successfulRequests > 0) {
                averageResponseTime = (double) totalResponseTime / successfulRequests;
            }
            
            int totalProcessed = successfulRequests + failedRequests;
            if (totalProcessed > 0) {
                successRate = (double) successfulRequests / totalProcessed * 100;
            }
            
            if (minResponseTime == Long.MAX_VALUE) {
                minResponseTime = 0;
            }
        }
        
        // Getters and Setters
        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public int getConcurrentRequests() { return concurrentRequests; }
        public void setConcurrentRequests(int concurrentRequests) { this.concurrentRequests = concurrentRequests; }
        
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getSuccessfulRequests() { return successfulRequests; }
        public int getFailedRequests() { return failedRequests; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getSuccessRate() { return successRate; }
        public long getMinResponseTime() { return minResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public List<RequestResult> getRequestResults() { return requestResults; }
    }
    
    /**
     * 单个请求结果类
     */
    public static class RequestResult {
        private long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;
        private int responseSize;
        
        public long getResponseTime() {
            return endTime - startTime;
        }
        
        // Getters and Setters
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public int getResponseSize() { return responseSize; }
        public void setResponseSize(int responseSize) { this.responseSize = responseSize; }
    }
}