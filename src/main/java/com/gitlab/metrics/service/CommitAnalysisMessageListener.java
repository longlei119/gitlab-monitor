package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.service.webhook.WebhookEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 代码提交分析消息监听器
 * 监听RabbitMQ中的提交分析队列，处理代码提交数据
 */
@Service
public class CommitAnalysisMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitAnalysisMessageListener.class);
    
    @Autowired
    private CommitAnalysisService commitAnalysisService;
    
    /**
     * 监听提交分析队列，处理push事件
     * 
     * @param message 包含push事件数据的消息
     */
    @RabbitListener(queues = "commit.analysis")
    public void handleCommitAnalysis(WebhookEventProcessor.WebhookEventMessage message) {
        String requestId = message.getRequestId();
        
        // 设置MDC用于日志跟踪
        MDC.put("requestId", requestId);
        
        try {
            logger.info("Received commit analysis message: requestId={}", requestId);
            
            // 验证消息数据
            if (message.getEventData() == null) {
                logger.error("Event data is null in commit analysis message: requestId={}", requestId);
                return;
            }
            
            if (!(message.getEventData() instanceof PushEventRequest)) {
                logger.error("Invalid event data type in commit analysis message: expected PushEventRequest, got {}, requestId={}", 
                           message.getEventData().getClass().getSimpleName(), requestId);
                return;
            }
            
            PushEventRequest pushEvent = (PushEventRequest) message.getEventData();
            
            // 处理提交分析
            int processedCommits = commitAnalysisService.processPushEvent(pushEvent);
            
            logger.info("Successfully processed commit analysis: requestId={}, processedCommits={}", 
                       requestId, processedCommits);
            
        } catch (Exception e) {
            logger.error("Failed to process commit analysis message: requestId={}", requestId, e);
            // 在生产环境中，可能需要实现重试机制或将消息发送到死信队列
            
        } finally {
            // 清理MDC
            MDC.clear();
        }
    }
}