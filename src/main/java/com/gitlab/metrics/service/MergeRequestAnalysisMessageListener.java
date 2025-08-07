package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.service.webhook.WebhookEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 合并请求分析消息监听器
 * 异步处理合并请求事件，实现评审流程管理
 */
@Component
public class MergeRequestAnalysisMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MergeRequestAnalysisMessageListener.class);
    
    @Autowired
    private MergeRequestService mergeRequestService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理合并请求分析消息
     */
    @RabbitListener(queues = "merge-request-analysis-queue")
    public void handleMergeRequestAnalysis(WebhookEventProcessor.WebhookEventMessage message) {
        String requestId = message.getRequestId();
        
        // Set up MDC for logging
        MDC.put("requestId", requestId);
        
        try {
            logger.info("Received merge request analysis message: requestId={}", requestId);
            
            // Extract the event data
            Object eventData = message.getEventData();
            MergeRequestEventRequest event;
            
            if (eventData instanceof MergeRequestEventRequest) {
                event = (MergeRequestEventRequest) eventData;
            } else {
                // Convert from generic object to specific type
                String eventJson = objectMapper.writeValueAsString(eventData);
                event = objectMapper.readValue(eventJson, MergeRequestEventRequest.class);
            }
            
            // 处理合并请求事件
            mergeRequestService.processMergeRequestEvent(event);
            
            logger.info("Successfully processed merge request analysis for MR: {}, requestId={}", 
                       event.getObjectAttributes().getId(), requestId);
            
        } catch (Exception e) {
            logger.error("Failed to process merge request analysis message: requestId={}", requestId, e);
            // 这里可以添加重试逻辑或死信队列处理
            throw new RuntimeException("Failed to process merge request analysis: " + e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}