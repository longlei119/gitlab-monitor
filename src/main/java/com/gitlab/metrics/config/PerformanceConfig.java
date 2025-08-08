package com.gitlab.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 性能优化配置
 * 配置异步处理、线程池和批量操作参数
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "app.performance")
public class PerformanceConfig {
    
    private int batchSize = 1000;
    private int asyncCorePoolSize = 5;
    private int asyncMaxPoolSize = 20;
    private int asyncQueueCapacity = 100;
    private int queryTimeout = 30;
    
    /**
     * 异步任务执行器
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncCorePoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setThreadNamePrefix("GitLabMetrics-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * 数据库批量操作执行器
     */
    @Bean("batchExecutor")
    public Executor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("BatchProcessor-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    // Getters and Setters
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getAsyncCorePoolSize() {
        return asyncCorePoolSize;
    }
    
    public void setAsyncCorePoolSize(int asyncCorePoolSize) {
        this.asyncCorePoolSize = asyncCorePoolSize;
    }
    
    public int getAsyncMaxPoolSize() {
        return asyncMaxPoolSize;
    }
    
    public void setAsyncMaxPoolSize(int asyncMaxPoolSize) {
        this.asyncMaxPoolSize = asyncMaxPoolSize;
    }
    
    public int getAsyncQueueCapacity() {
        return asyncQueueCapacity;
    }
    
    public void setAsyncQueueCapacity(int asyncQueueCapacity) {
        this.asyncQueueCapacity = asyncQueueCapacity;
    }
    
    public int getQueryTimeout() {
        return queryTimeout;
    }
    
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
}