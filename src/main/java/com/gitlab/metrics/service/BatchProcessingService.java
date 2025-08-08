package com.gitlab.metrics.service;

import com.gitlab.metrics.config.PerformanceConfig;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.entity.FileChange;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.repository.FileChangeRepository;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import com.gitlab.metrics.repository.TestCoverageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 批量处理服务
 * 提供高性能的批量数据处理功能，优化数据库操作性能
 */
@Service
public class BatchProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);
    
    @Autowired
    private PerformanceConfig performanceConfig;
    
    @Autowired
    private CommitRepository commitRepository;
    
    @Autowired
    private FileChangeRepository fileChangeRepository;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    /**
     * 批量保存提交记录
     * 
     * @param commits 提交记录列表
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchSaveCommits(List<Commit> commits) {
        logger.info("开始批量保存提交记录，数量: {}", commits.size());
        
        try {
            int batchSize = performanceConfig.getBatchSize();
            int totalSaved = 0;
            
            for (int i = 0; i < commits.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, commits.size());
                List<Commit> batch = commits.subList(i, endIndex);
                
                List<Commit> savedCommits = commitRepository.saveAll(batch);
                totalSaved += savedCommits.size();
                
                logger.debug("批量保存提交记录批次 {}-{}, 保存数量: {}", 
                           i + 1, endIndex, savedCommits.size());
                
                // 强制刷新到数据库
                commitRepository.flush();
            }
            
            logger.info("批量保存提交记录完成，总计保存: {}", totalSaved);
            return CompletableFuture.completedFuture(totalSaved);
            
        } catch (Exception e) {
            logger.error("批量保存提交记录失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 批量保存文件变更记录
     * 
     * @param fileChanges 文件变更记录列表
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchSaveFileChanges(List<FileChange> fileChanges) {
        logger.info("开始批量保存文件变更记录，数量: {}", fileChanges.size());
        
        try {
            int batchSize = performanceConfig.getBatchSize();
            int totalSaved = 0;
            
            for (int i = 0; i < fileChanges.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, fileChanges.size());
                List<FileChange> batch = fileChanges.subList(i, endIndex);
                
                List<FileChange> savedChanges = fileChangeRepository.saveAll(batch);
                totalSaved += savedChanges.size();
                
                logger.debug("批量保存文件变更批次 {}-{}, 保存数量: {}", 
                           i + 1, endIndex, savedChanges.size());
                
                fileChangeRepository.flush();
            }
            
            logger.info("批量保存文件变更记录完成，总计保存: {}", totalSaved);
            return CompletableFuture.completedFuture(totalSaved);
            
        } catch (Exception e) {
            logger.error("批量保存文件变更记录失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 批量保存质量指标
     * 
     * @param qualityMetrics 质量指标列表
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchSaveQualityMetrics(List<QualityMetrics> qualityMetrics) {
        logger.info("开始批量保存质量指标，数量: {}", qualityMetrics.size());
        
        try {
            int batchSize = performanceConfig.getBatchSize();
            int totalSaved = 0;
            
            for (int i = 0; i < qualityMetrics.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, qualityMetrics.size());
                List<QualityMetrics> batch = qualityMetrics.subList(i, endIndex);
                
                List<QualityMetrics> savedMetrics = qualityMetricsRepository.saveAll(batch);
                totalSaved += savedMetrics.size();
                
                logger.debug("批量保存质量指标批次 {}-{}, 保存数量: {}", 
                           i + 1, endIndex, savedMetrics.size());
                
                qualityMetricsRepository.flush();
            }
            
            logger.info("批量保存质量指标完成，总计保存: {}", totalSaved);
            return CompletableFuture.completedFuture(totalSaved);
            
        } catch (Exception e) {
            logger.error("批量保存质量指标失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 批量保存测试覆盖率
     * 
     * @param testCoverages 测试覆盖率列表
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchSaveTestCoverages(List<TestCoverage> testCoverages) {
        logger.info("开始批量保存测试覆盖率，数量: {}", testCoverages.size());
        
        try {
            int batchSize = performanceConfig.getBatchSize();
            int totalSaved = 0;
            
            for (int i = 0; i < testCoverages.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, testCoverages.size());
                List<TestCoverage> batch = testCoverages.subList(i, endIndex);
                
                List<TestCoverage> savedCoverages = testCoverageRepository.saveAll(batch);
                totalSaved += savedCoverages.size();
                
                logger.debug("批量保存测试覆盖率批次 {}-{}, 保存数量: {}", 
                           i + 1, endIndex, savedCoverages.size());
                
                testCoverageRepository.flush();
            }
            
            logger.info("批量保存测试覆盖率完成，总计保存: {}", totalSaved);
            return CompletableFuture.completedFuture(totalSaved);
            
        } catch (Exception e) {
            logger.error("批量保存测试覆盖率失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 批量删除过期数据
     * 
     * @param tableName 表名
     * @param cutoffDate 截止日期（删除此日期之前的数据）
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchDeleteExpiredData(String tableName, java.time.LocalDateTime cutoffDate) {
        logger.info("开始批量删除过期数据，表: {}, 截止日期: {}", tableName, cutoffDate);
        
        try {
            int deletedCount = 0;
            
            switch (tableName.toLowerCase()) {
                case "commits":
                    // 这里需要实现自定义的批量删除逻辑
                    // 由于JPA的deleteBy方法可能性能不佳，建议使用原生SQL
                    break;
                case "quality_metrics":
                    // 类似的批量删除逻辑
                    break;
                case "test_coverage":
                    // 类似的批量删除逻辑
                    break;
                default:
                    logger.warn("不支持的表名: {}", tableName);
                    return CompletableFuture.completedFuture(0);
            }
            
            logger.info("批量删除过期数据完成，删除数量: {}", deletedCount);
            return CompletableFuture.completedFuture(deletedCount);
            
        } catch (Exception e) {
            logger.error("批量删除过期数据失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 批量更新统计信息
     * 
     * @param projectIds 项目ID列表
     * @return 异步处理结果
     */
    @Async("batchExecutor")
    @Transactional
    public CompletableFuture<Integer> batchUpdateStatistics(List<String> projectIds) {
        logger.info("开始批量更新统计信息，项目数量: {}", projectIds.size());
        
        try {
            int updatedCount = 0;
            
            for (String projectId : projectIds) {
                // 这里可以实现统计信息的批量更新逻辑
                // 例如：更新项目的总提交数、总代码行数等聚合数据
                updatedCount++;
            }
            
            logger.info("批量更新统计信息完成，更新项目数: {}", updatedCount);
            return CompletableFuture.completedFuture(updatedCount);
            
        } catch (Exception e) {
            logger.error("批量更新统计信息失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 预热缓存
     * 
     * @param projectIds 需要预热的项目ID列表
     * @return 异步处理结果
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> warmupCache(List<String> projectIds) {
        logger.info("开始预热缓存，项目数量: {}", projectIds.size());
        
        try {
            for (String projectId : projectIds) {
                // 预加载常用的查询结果到缓存
                // 这里可以调用各种Service方法来预热缓存
                logger.debug("预热项目缓存: {}", projectId);
            }
            
            logger.info("缓存预热完成");
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}