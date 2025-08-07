package com.gitlab.metrics.service;

import com.gitlab.metrics.config.SonarQubeProperties;
import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SonarQube分析服务
 * 负责触发代码质量分析、获取分析结果并存储到数据库
 */
@Service
public class SonarQubeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SonarQubeAnalysisService.class);
    
    @Autowired
    private SonarQubeClientService sonarQubeClientService;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    @Autowired
    private SonarQubeProperties sonarQubeProperties;
    
    @Autowired
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SecurityAnalysisService securityAnalysisService;
    
    @Autowired
    private AlertService alertService;
    
    /**
     * 分析项目代码质量
     * 
     * @param projectId GitLab项目ID
     * @param commitSha 提交SHA
     * @param sonarProjectKey SonarQube项目键
     * @return 质量指标实体
     */
    @Transactional
    public Optional<QualityMetrics> analyzeProjectQuality(String projectId, String commitSha, String sonarProjectKey) {
        try {
            logger.info("开始分析项目代码质量: projectId={}, commitSha={}, sonarProjectKey={}", 
                projectId, commitSha, sonarProjectKey);
            
            // 检查SonarQube分析是否启用
            if (!sonarQubeProperties.getAnalysis().isEnabled()) {
                logger.info("SonarQube分析已禁用，跳过分析");
                return Optional.empty();
            }
            
            // 检查项目是否存在
            if (!sonarQubeClientService.projectExists(sonarProjectKey)) {
                logger.warn("SonarQube项目不存在: {}", sonarProjectKey);
                return Optional.empty();
            }
            
            // 检查是否已经存在该提交的分析结果
            Optional<QualityMetrics> existingMetrics = qualityMetricsRepository.findByCommitSha(commitSha);
            if (existingMetrics.isPresent()) {
                logger.info("提交 {} 的质量分析结果已存在，跳过重复分析", commitSha);
                return existingMetrics;
            }
            
            // 创建质量指标实体
            QualityMetrics qualityMetrics = new QualityMetrics(projectId, commitSha, LocalDateTime.now());
            
            // 获取项目质量指标
            Optional<Map<String, Object>> measuresResponse = 
                sonarQubeClientService.getProjectMeasures(sonarProjectKey);
            
            if (measuresResponse.isPresent()) {
                processMeasures(qualityMetrics, measuresResponse.get());
            }
            
            // 获取质量门禁状态
            Optional<Map<String, Object>> qualityGateResponse = 
                sonarQubeClientService.getProjectQualityGateStatus(sonarProjectKey);
            
            if (qualityGateResponse.isPresent()) {
                processQualityGate(qualityMetrics, qualityGateResponse.get());
            }
            
            // 获取问题统计
            processIssues(qualityMetrics, sonarProjectKey);
            
            // 保存质量指标
            QualityMetrics savedMetrics = qualityMetricsRepository.save(qualityMetrics);
            
            // 执行安全和性能分析
            performSecurityAndPerformanceAnalysis(projectId, sonarProjectKey);
            
            // 检查质量阈值
            checkQualityThresholds(projectId, sonarProjectKey);
            
            logger.info("成功保存项目 {} 提交 {} 的质量分析结果", projectId, commitSha);
            return Optional.of(savedMetrics);
            
        } catch (Exception e) {
            logger.error("分析项目代码质量失败: projectId={}, commitSha={}, error={}", 
                projectId, commitSha, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 处理SonarQube指标数据
     */
    private void processMeasures(QualityMetrics qualityMetrics, Map<String, Object> measures) {
        try {
            logger.debug("处理SonarQube指标数据");
            
            measures.forEach((metricKey, value) -> {
                if (value == null) {
                    return;
                }
                
                String valueStr = value.toString();
                if (valueStr.isEmpty()) {
                    return;
                }
                
                try {
                    switch (metricKey) {
                        case "complexity":
                            qualityMetrics.setCodeComplexity(Double.parseDouble(valueStr));
                            break;
                        case "duplicated_lines_density":
                            qualityMetrics.setDuplicateRate(Double.parseDouble(valueStr));
                            break;
                        case "maintainability_rating":
                            // SonarQube评级：1=A, 2=B, 3=C, 4=D, 5=E
                            // 转换为可维护性指数：A=100, B=80, C=60, D=40, E=20
                            double rating = Double.parseDouble(valueStr);
                            double maintainabilityIndex = Math.max(0, (6 - rating) * 20);
                            qualityMetrics.setMaintainabilityIndex(maintainabilityIndex);
                            break;
                        case "sqale_index":
                            // 技术债务，单位：分钟，转换为小时
                            double debtMinutes = Double.parseDouble(valueStr);
                            qualityMetrics.setTechnicalDebt(debtMinutes / 60.0);
                            break;
                        case "bugs":
                            qualityMetrics.setBugs(Integer.parseInt(valueStr));
                            break;
                        case "vulnerabilities":
                            qualityMetrics.setVulnerabilities(Integer.parseInt(valueStr));
                            break;
                        case "security_hotspots":
                            qualityMetrics.setHotspots(Integer.parseInt(valueStr));
                            break;
                        case "code_smells":
                            qualityMetrics.setCodeSmells(Integer.parseInt(valueStr));
                            break;
                        default:
                            logger.debug("未处理的指标: {} = {}", metricKey, valueStr);
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("解析指标值失败: {} = {}", metricKey, valueStr);
                }
            });
            
            logger.debug("成功处理SonarQube指标数据");
            
        } catch (Exception e) {
            logger.error("处理SonarQube指标数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理质量门禁状态
     */
    private void processQualityGate(QualityMetrics qualityMetrics, Map<String, Object> response) {
        try {
            logger.debug("处理质量门禁状态");
            
            String status = (String) response.get("status");
            qualityMetrics.setQualityGate(status);
            
            // 构建分析详情
            StringBuilder details = new StringBuilder();
            details.append("Quality Gate: ").append(status).append("\n");
            
            if (response.containsKey("conditions")) {
                details.append("Conditions:\n");
                // 简化处理，将conditions转换为字符串
                details.append(response.get("conditions").toString()).append("\n");
            }
            
            qualityMetrics.setAnalysisDetails(details.toString());
            
            logger.debug("成功处理质量门禁状态: {}", status);
            
        } catch (Exception e) {
            logger.error("处理质量门禁状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理问题统计
     */
    private void processIssues(QualityMetrics qualityMetrics, String sonarProjectKey) {
        try {
            logger.debug("处理问题统计");
            
            // 统计安全问题
            Optional<Map<String, Object>> vulnerabilities = 
                sonarQubeClientService.getProjectVulnerabilities(sonarProjectKey);
            if (vulnerabilities.isPresent()) {
                int securityCount = (Integer) vulnerabilities.get().get("total");
                qualityMetrics.setSecurityIssues(securityCount);
            }
            
            // 统计性能问题（通过严重程度为MAJOR和CRITICAL的代码异味来估算）
            Optional<Map<String, Object>> majorIssues = 
                sonarQubeClientService.getProjectIssues(sonarProjectKey, "MAJOR,CRITICAL");
            if (majorIssues.isPresent()) {
                // 简化处理：将严重和主要问题视为性能问题
                int performanceCount = (Integer) majorIssues.get().get("total");
                qualityMetrics.setPerformanceIssues(performanceCount);
            }
            
            logger.debug("成功处理问题统计");
            
        } catch (Exception e) {
            logger.error("处理问题统计失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 触发代码质量分析
     * 通过消息队列异步处理分析任务
     */
    public boolean triggerQualityAnalysis(String projectId, String commitSha, String sonarProjectKey) {
        try {
            logger.info("触发代码质量分析: projectId={}, commitSha={}, sonarProjectKey={}", 
                projectId, commitSha, sonarProjectKey);
            
            // 检查分析是否启用
            if (!sonarQubeProperties.getAnalysis().isEnabled()) {
                logger.info("代码质量分析已禁用");
                return false;
            }
            
            // 创建分析消息
            SonarQubeAnalysisMessageListener.QualityAnalysisMessage message = 
                new SonarQubeAnalysisMessageListener.QualityAnalysisMessage(projectId, commitSha, sonarProjectKey);
            
            // 发送到消息队列进行异步处理
            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.GITLAB_EVENTS_EXCHANGE,
                RabbitMQConfig.QUALITY_ROUTING_KEY,
                messageJson
            );
            
            logger.info("成功发送代码质量分析消息到队列: projectId={}, commitSha={}", projectId, commitSha);
            return true;
            
        } catch (Exception e) {
            logger.error("触发代码质量分析失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查质量门禁是否通过
     */
    public boolean isQualityGatePassed(String sonarProjectKey) {
        try {
            Optional<Map<String, Object>> response = 
                sonarQubeClientService.getProjectQualityGateStatus(sonarProjectKey);
            
            if (response.isPresent()) {
                String status = (String) response.get().get("status");
                return "OK".equals(status);
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("检查质量门禁状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取项目的最新质量指标
     */
    public Optional<QualityMetrics> getLatestQualityMetrics(String projectId) {
        try {
            List<QualityMetrics> metrics = qualityMetricsRepository.findLatestByProject(projectId);
            return metrics.isEmpty() ? Optional.empty() : Optional.of(metrics.get(0));
            
        } catch (Exception e) {
            logger.error("获取最新质量指标失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 执行安全和性能分析
     */
    private void performSecurityAndPerformanceAnalysis(String projectId, String sonarProjectKey) {
        try {
            logger.info("开始执行安全和性能分析: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            // 异步执行安全分析
            try {
                SecurityAnalysisService.SecurityAnalysisResult securityResult = 
                    securityAnalysisService.analyzeSecurityVulnerabilities(projectId, sonarProjectKey);
                
                // 处理安全分析告警
                alertService.handleSecurityAnalysisAlert(securityResult);
                
            } catch (Exception e) {
                logger.error("安全分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
            }
            
            // 异步执行性能分析
            try {
                SecurityAnalysisService.PerformanceAnalysisResult performanceResult = 
                    securityAnalysisService.analyzePerformanceIssues(projectId, sonarProjectKey);
                
                // 处理性能分析告警
                alertService.handlePerformanceAnalysisAlert(performanceResult);
                
            } catch (Exception e) {
                logger.error("性能分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
            }
            
            logger.info("完成安全和性能分析: projectId={}", projectId);
            
        } catch (Exception e) {
            logger.error("执行安全和性能分析失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查质量阈值
     */
    private void checkQualityThresholds(String projectId, String sonarProjectKey) {
        try {
            logger.info("开始检查质量阈值: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            SecurityAnalysisService.QualityThresholdResult thresholdResult = 
                securityAnalysisService.checkQualityThresholds(projectId, sonarProjectKey);
            
            // 处理质量阈值告警
            alertService.handleQualityThresholdAlert(thresholdResult);
            
            logger.info("完成质量阈值检查: projectId={}, 是否阻止合并={}", 
                projectId, thresholdResult.isShouldBlockMerge());
            
        } catch (Exception e) {
            logger.error("检查质量阈值失败: projectId={}, error={}", projectId, e.getMessage(), e);
        }
    }
}