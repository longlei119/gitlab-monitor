package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 测试覆盖率分析消息监听器
 * 监听RabbitMQ中的测试覆盖率分析任务，异步处理覆盖率报告解析
 */
@Component
public class TestCoverageAnalysisMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCoverageAnalysisMessageListener.class);
    
    @Autowired
    private TestCoverageService testCoverageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 监听测试覆盖率分析队列
     */
    @RabbitListener(queues = "test.coverage.analysis")
    public void handleCoverageAnalysis(String message) {
        logger.info("收到测试覆盖率分析消息: {}", message);
        
        try {
            CoverageAnalysisMessage analysisMessage = objectMapper.readValue(message, CoverageAnalysisMessage.class);
            
            // 解析覆盖率报告
            testCoverageService.parseCoverageReport(
                analysisMessage.getProjectId(),
                analysisMessage.getCommitSha(),
                analysisMessage.getReportContent(),
                analysisMessage.getReportType(),
                analysisMessage.getReportPath()
            );
            
            logger.info("测试覆盖率分析完成: projectId={}, commitSha={}", 
                       analysisMessage.getProjectId(), analysisMessage.getCommitSha());
            
        } catch (Exception e) {
            logger.error("处理测试覆盖率分析消息失败: message={}, error={}", message, e.getMessage(), e);
        }
    }
    
    /**
     * 监听测试覆盖率文件分析队列
     */
    @RabbitListener(queues = "test.coverage.file.analysis")
    public void handleCoverageFileAnalysis(String message) {
        logger.info("收到测试覆盖率文件分析消息: {}", message);
        
        try {
            CoverageFileAnalysisMessage analysisMessage = objectMapper.readValue(message, CoverageFileAnalysisMessage.class);
            
            // 从文件解析覆盖率报告
            testCoverageService.parseCoverageReportFromFile(
                analysisMessage.getProjectId(),
                analysisMessage.getCommitSha(),
                analysisMessage.getFilePath(),
                analysisMessage.getReportType()
            );
            
            logger.info("测试覆盖率文件分析完成: projectId={}, commitSha={}, filePath={}", 
                       analysisMessage.getProjectId(), analysisMessage.getCommitSha(), analysisMessage.getFilePath());
            
        } catch (Exception e) {
            logger.error("处理测试覆盖率文件分析消息失败: message={}, error={}", message, e.getMessage(), e);
        }
    }
    
    /**
     * 测试覆盖率分析消息
     */
    public static class CoverageAnalysisMessage {
        private String projectId;
        private String commitSha;
        private String reportContent;
        private String reportType;
        private String reportPath;
        
        public CoverageAnalysisMessage() {}
        
        public CoverageAnalysisMessage(String projectId, String commitSha, String reportContent, 
                                     String reportType, String reportPath) {
            this.projectId = projectId;
            this.commitSha = commitSha;
            this.reportContent = reportContent;
            this.reportType = reportType;
            this.reportPath = reportPath;
        }
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getCommitSha() { return commitSha; }
        public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
        
        public String getReportContent() { return reportContent; }
        public void setReportContent(String reportContent) { this.reportContent = reportContent; }
        
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        
        public String getReportPath() { return reportPath; }
        public void setReportPath(String reportPath) { this.reportPath = reportPath; }
    }
    
    /**
     * 测试覆盖率文件分析消息
     */
    public static class CoverageFileAnalysisMessage {
        private String projectId;
        private String commitSha;
        private String filePath;
        private String reportType;
        
        public CoverageFileAnalysisMessage() {}
        
        public CoverageFileAnalysisMessage(String projectId, String commitSha, String filePath, String reportType) {
            this.projectId = projectId;
            this.commitSha = commitSha;
            this.filePath = filePath;
            this.reportType = reportType;
        }
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getCommitSha() { return commitSha; }
        public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
    }
}