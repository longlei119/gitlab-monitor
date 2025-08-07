package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gitlab.metrics.entity.QualityMetrics;
import com.gitlab.metrics.repository.QualityMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 安全分析服务
 * 负责检测和分析代码中的安全漏洞和性能问题
 */
@Service
public class SecurityAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAnalysisService.class);
    
    // 安全漏洞严重程度分类
    public enum SecuritySeverity {
        CRITICAL("CRITICAL", 5),
        HIGH("HIGH", 4),
        MEDIUM("MEDIUM", 3),
        LOW("LOW", 2),
        INFO("INFO", 1);
        
        private final String level;
        private final int priority;
        
        SecuritySeverity(String level, int priority) {
            this.level = level;
            this.priority = priority;
        }
        
        public String getLevel() {
            return level;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public static SecuritySeverity fromString(String level) {
            for (SecuritySeverity severity : values()) {
                if (severity.level.equalsIgnoreCase(level)) {
                    return severity;
                }
            }
            return INFO;
        }
    }
    
    // 性能问题类型
    public enum PerformanceIssueType {
        MEMORY_LEAK("内存泄漏"),
        CPU_INTENSIVE("CPU密集"),
        IO_BLOCKING("IO阻塞"),
        DATABASE_QUERY("数据库查询"),
        NETWORK_LATENCY("网络延迟"),
        ALGORITHM_COMPLEXITY("算法复杂度");
        
        private final String description;
        
        PerformanceIssueType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Autowired
    private SonarQubeClientService sonarQubeClientService;
    
    @Autowired
    private QualityMetricsRepository qualityMetricsRepository;
    
    /**
     * 分析项目的安全漏洞
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 安全分析结果
     */
    public SecurityAnalysisResult analyzeSecurityVulnerabilities(String projectId, String sonarProjectKey) {
        try {
            logger.info("开始分析项目安全漏洞: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            SecurityAnalysisResult result = new SecurityAnalysisResult();
            result.setProjectId(projectId);
            result.setSonarProjectKey(sonarProjectKey);
            result.setAnalysisTime(LocalDateTime.now());
            
            // 获取安全漏洞
            Optional<Map<String, Object>> vulnerabilities = 
                sonarQubeClientService.getProjectVulnerabilities(sonarProjectKey);
            
            if (vulnerabilities.isPresent()) {
                processSecurityVulnerabilities(result, vulnerabilities.get());
            }
            
            // 获取安全热点
            Optional<Map<String, Object>> hotspots = 
                sonarQubeClientService.getProjectIssues(sonarProjectKey, null);
            
            if (hotspots.isPresent()) {
                processSecurityHotspots(result, hotspots.get());
            }
            
            // 计算安全风险评分
            calculateSecurityRiskScore(result);
            
            logger.info("完成项目安全漏洞分析: projectId={}, 漏洞数量={}, 风险评分={}", 
                projectId, result.getTotalVulnerabilities(), result.getRiskScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("分析项目安全漏洞失败: projectId={}, error={}", projectId, e.getMessage(), e);
            throw new RuntimeException("安全漏洞分析失败", e);
        }
    }
    
    /**
     * 分析项目的性能问题
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 性能分析结果
     */
    public PerformanceAnalysisResult analyzePerformanceIssues(String projectId, String sonarProjectKey) {
        try {
            logger.info("开始分析项目性能问题: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            PerformanceAnalysisResult result = new PerformanceAnalysisResult();
            result.setProjectId(projectId);
            result.setSonarProjectKey(sonarProjectKey);
            result.setAnalysisTime(LocalDateTime.now());
            
            // 获取代码异味（可能包含性能问题）
            Optional<Map<String, Object>> codeSmells = 
                sonarQubeClientService.getProjectCodeSmells(sonarProjectKey);
            
            if (codeSmells.isPresent()) {
                processPerformanceIssues(result, codeSmells.get());
            }
            
            // 获取复杂度相关问题
            Optional<Map<String, Object>> measures = 
                sonarQubeClientService.getProjectMeasures(sonarProjectKey);
            
            if (measures.isPresent()) {
                processComplexityIssues(result, measures.get());
            }
            
            // 计算性能风险评分
            calculatePerformanceRiskScore(result);
            
            logger.info("完成项目性能问题分析: projectId={}, 问题数量={}, 风险评分={}", 
                projectId, result.getTotalIssues(), result.getRiskScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("分析项目性能问题失败: projectId={}, error={}", projectId, e.getMessage(), e);
            throw new RuntimeException("性能问题分析失败", e);
        }
    }
    
    /**
     * 检查质量阈值
     * 
     * @param projectId 项目ID
     * @param sonarProjectKey SonarQube项目键
     * @return 质量阈值检查结果
     */
    public QualityThresholdResult checkQualityThresholds(String projectId, String sonarProjectKey) {
        try {
            logger.info("开始检查质量阈值: projectId={}, sonarProjectKey={}", projectId, sonarProjectKey);
            
            QualityThresholdResult result = new QualityThresholdResult();
            result.setProjectId(projectId);
            result.setSonarProjectKey(sonarProjectKey);
            result.setCheckTime(LocalDateTime.now());
            
            // 获取最新的质量指标
            Optional<QualityMetrics> latestMetrics = getLatestQualityMetrics(projectId);
            
            if (latestMetrics.isPresent()) {
                QualityMetrics metrics = latestMetrics.get();
                
                // 检查各项阈值
                checkSecurityThresholds(result, metrics);
                checkPerformanceThresholds(result, metrics);
                checkQualityGateThresholds(result, metrics);
                checkCoverageThresholds(result, metrics);
                
                // 判断是否应该阻止合并
                result.setShouldBlockMerge(shouldBlockMerge(result));
                
                logger.info("完成质量阈值检查: projectId={}, 是否阻止合并={}", 
                    projectId, result.isShouldBlockMerge());
            } else {
                logger.warn("未找到项目 {} 的质量指标数据", projectId);
                result.setShouldBlockMerge(false);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("检查质量阈值失败: projectId={}, error={}", projectId, e.getMessage(), e);
            throw new RuntimeException("质量阈值检查失败", e);
        }
    }
    
    /**
     * 处理安全漏洞数据
     */
    private void processSecurityVulnerabilities(SecurityAnalysisResult result, Map<String, Object> vulnerabilities) {
        try {
            int totalCount = (Integer) vulnerabilities.get("total");
            result.setTotalVulnerabilities(totalCount);
            
            if (vulnerabilities.containsKey("issues")) {
                JsonNode issues = (JsonNode) vulnerabilities.get("issues");
                Map<SecuritySeverity, Integer> severityCount = new HashMap<>();
                
                for (JsonNode issue : issues) {
                    String severity = issue.get("severity").asText();
                    SecuritySeverity securitySeverity = SecuritySeverity.fromString(severity);
                    severityCount.put(securitySeverity, severityCount.getOrDefault(securitySeverity, 0) + 1);
                    
                    // 记录详细信息
                    SecurityVulnerability vulnerability = new SecurityVulnerability();
                    vulnerability.setKey(issue.get("key").asText());
                    vulnerability.setRule(issue.get("rule").asText());
                    vulnerability.setSeverity(securitySeverity);
                    vulnerability.setMessage(issue.get("message").asText());
                    vulnerability.setComponent(issue.get("component").asText());
                    
                    result.getVulnerabilities().add(vulnerability);
                }
                
                result.setSeverityDistribution(severityCount);
            }
            
        } catch (Exception e) {
            logger.error("处理安全漏洞数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理安全热点数据
     */
    private void processSecurityHotspots(SecurityAnalysisResult result, Map<String, Object> hotspots) {
        try {
            // 简化处理：从总问题中筛选安全相关的热点
            if (hotspots.containsKey("issues")) {
                JsonNode issues = (JsonNode) hotspots.get("issues");
                int hotspotCount = 0;
                
                for (JsonNode issue : issues) {
                    String type = issue.get("type").asText();
                    if ("SECURITY_HOTSPOT".equals(type)) {
                        hotspotCount++;
                    }
                }
                
                result.setSecurityHotspots(hotspotCount);
            }
            
        } catch (Exception e) {
            logger.error("处理安全热点数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理性能问题数据
     */
    private void processPerformanceIssues(PerformanceAnalysisResult result, Map<String, Object> codeSmells) {
        try {
            int totalCount = (Integer) codeSmells.get("total");
            
            if (codeSmells.containsKey("issues")) {
                JsonNode issues = (JsonNode) codeSmells.get("issues");
                Map<PerformanceIssueType, Integer> typeCount = new HashMap<>();
                
                for (JsonNode issue : issues) {
                    String rule = issue.get("rule").asText();
                    String message = issue.get("message").asText().toLowerCase();
                    
                    // 根据规则和消息内容判断性能问题类型
                    PerformanceIssueType issueType = classifyPerformanceIssue(rule, message);
                    if (issueType != null) {
                        typeCount.put(issueType, typeCount.getOrDefault(issueType, 0) + 1);
                        
                        // 记录详细信息
                        PerformanceIssue performanceIssue = new PerformanceIssue();
                        performanceIssue.setKey(issue.get("key").asText());
                        performanceIssue.setRule(rule);
                        performanceIssue.setType(issueType);
                        performanceIssue.setMessage(issue.get("message").asText());
                        performanceIssue.setComponent(issue.get("component").asText());
                        performanceIssue.setSeverity(issue.get("severity").asText());
                        
                        result.getPerformanceIssues().add(performanceIssue);
                    }
                }
                
                result.setTotalIssues(result.getPerformanceIssues().size());
                result.setIssueTypeDistribution(typeCount);
            }
            
        } catch (Exception e) {
            logger.error("处理性能问题数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理复杂度问题
     */
    private void processComplexityIssues(PerformanceAnalysisResult result, Map<String, Object> measures) {
        try {
            if (measures.containsKey("complexity")) {
                double complexity = Double.parseDouble(measures.get("complexity").toString());
                result.setComplexityScore(complexity);
                
                // 如果复杂度过高，添加到性能问题中
                if (complexity > 10.0) { // 阈值可配置
                    PerformanceIssue complexityIssue = new PerformanceIssue();
                    complexityIssue.setKey("complexity-high");
                    complexityIssue.setRule("complexity");
                    complexityIssue.setType(PerformanceIssueType.ALGORITHM_COMPLEXITY);
                    complexityIssue.setMessage("代码复杂度过高: " + complexity);
                    complexityIssue.setSeverity("MAJOR");
                    
                    result.getPerformanceIssues().add(complexityIssue);
                    result.setTotalIssues(result.getTotalIssues() + 1);
                }
            }
            
        } catch (Exception e) {
            logger.error("处理复杂度问题失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 分类性能问题类型
     */
    private PerformanceIssueType classifyPerformanceIssue(String rule, String message) {
        // 根据规则名称和消息内容判断性能问题类型
        if (rule.contains("memory") || message.contains("memory") || message.contains("内存")) {
            return PerformanceIssueType.MEMORY_LEAK;
        } else if (rule.contains("cpu") || message.contains("cpu") || message.contains("处理器")) {
            return PerformanceIssueType.CPU_INTENSIVE;
        } else if (rule.contains("io") || message.contains("io") || message.contains("输入输出")) {
            return PerformanceIssueType.IO_BLOCKING;
        } else if (rule.contains("sql") || rule.contains("database") || message.contains("数据库")) {
            return PerformanceIssueType.DATABASE_QUERY;
        } else if (rule.contains("network") || message.contains("network") || message.contains("网络")) {
            return PerformanceIssueType.NETWORK_LATENCY;
        } else if (rule.contains("complexity") || message.contains("complexity") || message.contains("复杂")) {
            return PerformanceIssueType.ALGORITHM_COMPLEXITY;
        }
        
        return null; // 不是性能相关问题
    }
    
    /**
     * 计算安全风险评分
     */
    private void calculateSecurityRiskScore(SecurityAnalysisResult result) {
        double score = 0.0;
        
        if (result.getSeverityDistribution() != null) {
            for (Map.Entry<SecuritySeverity, Integer> entry : result.getSeverityDistribution().entrySet()) {
                score += entry.getKey().getPriority() * entry.getValue();
            }
        }
        
        // 加上安全热点的权重
        score += result.getSecurityHotspots() * 2.0;
        
        result.setRiskScore(score);
    }
    
    /**
     * 计算性能风险评分
     */
    private void calculatePerformanceRiskScore(PerformanceAnalysisResult result) {
        double score = 0.0;
        
        // 基于问题数量和复杂度计算评分
        score += result.getTotalIssues() * 2.0;
        score += result.getComplexityScore() * 0.5;
        
        result.setRiskScore(score);
    }
    
    /**
     * 检查安全阈值
     */
    private void checkSecurityThresholds(QualityThresholdResult result, QualityMetrics metrics) {
        List<ThresholdViolation> violations = new ArrayList<>();
        
        // 检查安全漏洞数量阈值
        if (metrics.getVulnerabilities() != null && metrics.getVulnerabilities() > 0) {
            violations.add(new ThresholdViolation(
                "security_vulnerabilities",
                "安全漏洞数量",
                metrics.getVulnerabilities().toString(),
                "0",
                "CRITICAL"
            ));
        }
        
        // 检查安全热点数量阈值
        if (metrics.getHotspots() != null && metrics.getHotspots() > 5) {
            violations.add(new ThresholdViolation(
                "security_hotspots",
                "安全热点数量",
                metrics.getHotspots().toString(),
                "5",
                "HIGH"
            ));
        }
        
        result.getSecurityViolations().addAll(violations);
    }
    
    /**
     * 检查性能阈值
     */
    private void checkPerformanceThresholds(QualityThresholdResult result, QualityMetrics metrics) {
        List<ThresholdViolation> violations = new ArrayList<>();
        
        // 检查代码复杂度阈值
        if (metrics.getCodeComplexity() != null && metrics.getCodeComplexity() > 10.0) {
            violations.add(new ThresholdViolation(
                "code_complexity",
                "代码复杂度",
                metrics.getCodeComplexity().toString(),
                "10.0",
                "MAJOR"
            ));
        }
        
        // 检查性能问题数量阈值
        if (metrics.getPerformanceIssues() != null && metrics.getPerformanceIssues() > 10) {
            violations.add(new ThresholdViolation(
                "performance_issues",
                "性能问题数量",
                metrics.getPerformanceIssues().toString(),
                "10",
                "MAJOR"
            ));
        }
        
        result.getPerformanceViolations().addAll(violations);
    }
    
    /**
     * 检查质量门禁阈值
     */
    private void checkQualityGateThresholds(QualityThresholdResult result, QualityMetrics metrics) {
        if (metrics.getQualityGate() != null && !"OK".equals(metrics.getQualityGate())) {
            result.getQualityGateViolations().add(new ThresholdViolation(
                "quality_gate",
                "质量门禁",
                metrics.getQualityGate(),
                "OK",
                "CRITICAL"
            ));
        }
    }
    
    /**
     * 检查测试覆盖率阈值
     */
    private void checkCoverageThresholds(QualityThresholdResult result, QualityMetrics metrics) {
        // 这里需要从TestCoverage实体获取覆盖率数据
        // 简化处理，假设有覆盖率数据
        // TODO: 集成TestCoverage数据
    }
    
    /**
     * 判断是否应该阻止合并
     */
    private boolean shouldBlockMerge(QualityThresholdResult result) {
        // 如果有CRITICAL级别的违规，阻止合并
        return result.getSecurityViolations().stream().anyMatch(v -> "CRITICAL".equals(v.getSeverity())) ||
               result.getQualityGateViolations().stream().anyMatch(v -> "CRITICAL".equals(v.getSeverity()));
    }
    
    /**
     * 获取最新的质量指标
     */
    private Optional<QualityMetrics> getLatestQualityMetrics(String projectId) {
        try {
            List<QualityMetrics> metrics = qualityMetricsRepository.findLatestByProject(projectId);
            return metrics.isEmpty() ? Optional.empty() : Optional.of(metrics.get(0));
        } catch (Exception e) {
            logger.error("获取最新质量指标失败: projectId={}, error={}", projectId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    // 内部类定义
    
    /**
     * 安全分析结果
     */
    public static class SecurityAnalysisResult {
        private String projectId;
        private String sonarProjectKey;
        private LocalDateTime analysisTime;
        private int totalVulnerabilities;
        private int securityHotspots;
        private double riskScore;
        private Map<SecuritySeverity, Integer> severityDistribution = new HashMap<>();
        private List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getSonarProjectKey() { return sonarProjectKey; }
        public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }
        
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
        
        public int getTotalVulnerabilities() { return totalVulnerabilities; }
        public void setTotalVulnerabilities(int totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; }
        
        public int getSecurityHotspots() { return securityHotspots; }
        public void setSecurityHotspots(int securityHotspots) { this.securityHotspots = securityHotspots; }
        
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        
        public Map<SecuritySeverity, Integer> getSeverityDistribution() { return severityDistribution; }
        public void setSeverityDistribution(Map<SecuritySeverity, Integer> severityDistribution) { this.severityDistribution = severityDistribution; }
        
        public List<SecurityVulnerability> getVulnerabilities() { return vulnerabilities; }
        public void setVulnerabilities(List<SecurityVulnerability> vulnerabilities) { this.vulnerabilities = vulnerabilities; }
    }
    
    /**
     * 性能分析结果
     */
    public static class PerformanceAnalysisResult {
        private String projectId;
        private String sonarProjectKey;
        private LocalDateTime analysisTime;
        private int totalIssues;
        private double complexityScore;
        private double riskScore;
        private Map<PerformanceIssueType, Integer> issueTypeDistribution = new HashMap<>();
        private List<PerformanceIssue> performanceIssues = new ArrayList<>();
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getSonarProjectKey() { return sonarProjectKey; }
        public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }
        
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
        
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
        
        public double getComplexityScore() { return complexityScore; }
        public void setComplexityScore(double complexityScore) { this.complexityScore = complexityScore; }
        
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        
        public Map<PerformanceIssueType, Integer> getIssueTypeDistribution() { return issueTypeDistribution; }
        public void setIssueTypeDistribution(Map<PerformanceIssueType, Integer> issueTypeDistribution) { this.issueTypeDistribution = issueTypeDistribution; }
        
        public List<PerformanceIssue> getPerformanceIssues() { return performanceIssues; }
        public void setPerformanceIssues(List<PerformanceIssue> performanceIssues) { this.performanceIssues = performanceIssues; }
    }
    
    /**
     * 质量阈值检查结果
     */
    public static class QualityThresholdResult {
        private String projectId;
        private String sonarProjectKey;
        private LocalDateTime checkTime;
        private boolean shouldBlockMerge;
        private List<ThresholdViolation> securityViolations = new ArrayList<>();
        private List<ThresholdViolation> performanceViolations = new ArrayList<>();
        private List<ThresholdViolation> qualityGateViolations = new ArrayList<>();
        private List<ThresholdViolation> coverageViolations = new ArrayList<>();
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getSonarProjectKey() { return sonarProjectKey; }
        public void setSonarProjectKey(String sonarProjectKey) { this.sonarProjectKey = sonarProjectKey; }
        
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        public boolean isShouldBlockMerge() { return shouldBlockMerge; }
        public void setShouldBlockMerge(boolean shouldBlockMerge) { this.shouldBlockMerge = shouldBlockMerge; }
        
        public List<ThresholdViolation> getSecurityViolations() { return securityViolations; }
        public void setSecurityViolations(List<ThresholdViolation> securityViolations) { this.securityViolations = securityViolations; }
        
        public List<ThresholdViolation> getPerformanceViolations() { return performanceViolations; }
        public void setPerformanceViolations(List<ThresholdViolation> performanceViolations) { this.performanceViolations = performanceViolations; }
        
        public List<ThresholdViolation> getQualityGateViolations() { return qualityGateViolations; }
        public void setQualityGateViolations(List<ThresholdViolation> qualityGateViolations) { this.qualityGateViolations = qualityGateViolations; }
        
        public List<ThresholdViolation> getCoverageViolations() { return coverageViolations; }
        public void setCoverageViolations(List<ThresholdViolation> coverageViolations) { this.coverageViolations = coverageViolations; }
    }
    
    /**
     * 安全漏洞详情
     */
    public static class SecurityVulnerability {
        private String key;
        private String rule;
        private SecuritySeverity severity;
        private String message;
        private String component;
        
        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        
        public SecuritySeverity getSeverity() { return severity; }
        public void setSeverity(SecuritySeverity severity) { this.severity = severity; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
    }
    
    /**
     * 性能问题详情
     */
    public static class PerformanceIssue {
        private String key;
        private String rule;
        private PerformanceIssueType type;
        private String message;
        private String component;
        private String severity;
        
        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        
        public PerformanceIssueType getType() { return type; }
        public void setType(PerformanceIssueType type) { this.type = type; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    /**
     * 阈值违规详情
     */
    public static class ThresholdViolation {
        private String metric;
        private String description;
        private String actualValue;
        private String expectedValue;
        private String severity;
        
        public ThresholdViolation(String metric, String description, String actualValue, String expectedValue, String severity) {
            this.metric = metric;
            this.description = description;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
            this.severity = severity;
        }
        
        // Getters and Setters
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getActualValue() { return actualValue; }
        public void setActualValue(String actualValue) { this.actualValue = actualValue; }
        
        public String getExpectedValue() { return expectedValue; }
        public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
}