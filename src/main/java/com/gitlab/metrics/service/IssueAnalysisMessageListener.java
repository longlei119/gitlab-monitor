package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.IssueEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Issue分析消息监听器
 * 处理异步的Issue分析任务
 */
@Service
public class IssueAnalysisMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(IssueAnalysisMessageListener.class);
    
    @Autowired
    private IssueAnalysisService issueAnalysisService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理Issue分析消息
     */
    @RabbitListener(queues = "bug.tracking.analysis.queue")
    public void handleIssueAnalysisMessage(String message) {
        try {
            logger.info("收到Issue分析消息: {}", message);
            
            // 解析WebhookEventMessage包装器
            com.gitlab.metrics.service.webhook.WebhookEventProcessor.WebhookEventMessage eventMessage = 
                objectMapper.readValue(message, com.gitlab.metrics.service.webhook.WebhookEventProcessor.WebhookEventMessage.class);
            
            // 提取IssueEventRequest
            IssueEventRequest issueEvent = objectMapper.convertValue(eventMessage.getEventData(), IssueEventRequest.class);
            
            // 执行Issue分析
            IssueAnalysisService.IssueProcessResult result = 
                issueAnalysisService.processIssueEvent(issueEvent);
            
            if (result.isSuccess()) {
                logger.info("成功处理Issue分析消息: issueId={}, action={}, requestId={}", 
                    result.getIssueId(), result.getAction(), eventMessage.getRequestId());
            } else {
                logger.error("处理Issue分析消息失败: issueId={}, message={}, requestId={}", 
                    result.getIssueId(), result.getMessage(), eventMessage.getRequestId());
            }
            
        } catch (Exception e) {
            logger.error("处理Issue分析消息失败: {}", message, e);
            // 这里可以实现重试机制或者将失败的消息发送到死信队列
        }
    }
}