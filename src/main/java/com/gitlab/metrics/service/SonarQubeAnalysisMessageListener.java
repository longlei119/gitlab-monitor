package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SonarQube分析消息监听器
 * 处理异步的代码质量分析任务
 */
@Service
public class SonarQubeAnalysisMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SonarQubeAnalysisMessageListener.class);
    
    @Autowired
    private SonarQubeAnalysisService sonarQubeAnalysisService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理代码质量分析消息
     */
    @RabbitListener(queues = "quality.analysis.queue")
    public void handleQualityAnalysisMessage(String message) {
        try {
            logger.info("收到代码质量分析消息: {}", message);
            
            // 解析消息
            QualityAnalysisMessage analysisMessage = objectMapper.readValue(message, QualityAnalysisMessage.class);
            
            // 执行质量分析
            sonarQubeAnalysisService.analyzeProjectQuality(
                analysisMessage.getProjectId(),
                analysisMessage.getCommitSha(),
                analysisMessage.getSonarProjectKey()
            );
            
            logger.info("成功处理代码质量分析消息: projectId={}, commitSha={}", 
                analysisMessage.getProjectId(), analysisMessage.getCommitSha());
            
        } catch (Exception e) {
            logger.error("处理代码质量分析消息失败: {}", message, e);
            // 这里可以实现重试机制或者将失败的消息发送到死信队列
        }
    }
    
    /**
     * 质量分析消息实体类
     */
    public static class QualityAnalysisMessage {
        private String projectId;
        private String commitSha;
        private String sonarProjectKey;
        private String branch;
        private Long timestamp;
        
        public QualityAnalysisMessage() {}
        
        public QualityAnalysisMessage(String projectId, String commitSha, String sonarProjectKey) {
            this.projectId = projectId;
            this.commitSha = commitSha;
            this.sonarProjectKey = sonarProjectKey;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getProjectId() {
            return projectId;
        }
        
        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }
        
        public String getCommitSha() {
            return commitSha;
        }
        
        public void setCommitSha(String commitSha) {
            this.commitSha = commitSha;
        }
        
        public String getSonarProjectKey() {
            return sonarProjectKey;
        }
        
        public void setSonarProjectKey(String sonarProjectKey) {
            this.sonarProjectKey = sonarProjectKey;
        }
        
        public String getBranch() {
            return branch;
        }
        
        public void setBranch(String branch) {
            this.branch = branch;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "QualityAnalysisMessage{" +
                    "projectId='" + projectId + '\'' +
                    ", commitSha='" + commitSha + '\'' +
                    ", sonarProjectKey='" + sonarProjectKey + '\'' +
                    ", branch='" + branch + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}