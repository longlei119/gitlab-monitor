package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.config.SonarQubeProperties;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SonarQube客户端服务
 * 提供与SonarQube API交互的方法
 */
@Service
public class SonarQubeClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(SonarQubeClientService.class);
    
    // SonarQube指标键
    private static final List<String> QUALITY_METRICS = Arrays.asList(
        "complexity",                    // 复杂度
        "duplicated_lines_density",      // 重复行密度
        "maintainability_rating",        // 可维护性评级
        "reliability_rating",            // 可靠性评级
        "security_rating",               // 安全性评级
        "sqale_index",                   // 技术债务（分钟）
        "bugs",                          // Bug数量
        "vulnerabilities",               // 漏洞数量
        "security_hotspots",             // 安全热点
        "code_smells",                   // 代码异味
        "coverage",                      // 测试覆盖率
        "ncloc",                         // 代码行数
        "violations"                     // 违规总数
    );
    
    @Autowired
    private CloseableHttpClient httpClient;
    
    @Autowired
    private SonarQubeProperties sonarQubeProperties;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取项目的质量指标
     */
    public Optional<Map<String, Object>> getProjectMeasures(String projectKey) {
        try {
            logger.info("获取项目质量指标: {}", projectKey);
            
            String metricsParam = String.join(",", QUALITY_METRICS);
            String url = String.format("%s/api/measures/component?component=%s&metricKeys=%s",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(metricsParam, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode component = response.get().get("component");
                if (component != null && component.has("measures")) {
                    JsonNode measures = component.get("measures");
                    for (JsonNode measure : measures) {
                        String metric = measure.get("metric").asText();
                        String value = measure.has("value") ? measure.get("value").asText() : null;
                        if (value != null) {
                            result.put(metric, value);
                        }
                    }
                }
                logger.debug("成功获取项目 {} 的质量指标", projectKey);
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 质量指标失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的质量门禁状态
     */
    public Optional<Map<String, Object>> getProjectQualityGateStatus(String projectKey) {
        try {
            logger.info("获取项目质量门禁状态: {}", projectKey);
            
            String url = String.format("%s/api/qualitygates/project_status?projectKey=%s",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode projectStatus = response.get().get("projectStatus");
                if (projectStatus != null) {
                    result.put("status", projectStatus.get("status").asText());
                    if (projectStatus.has("conditions")) {
                        result.put("conditions", projectStatus.get("conditions"));
                    }
                }
                logger.debug("成功获取项目 {} 的质量门禁状态", projectKey);
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 质量门禁状态失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的问题列表
     */
    public Optional<Map<String, Object>> getProjectIssues(String projectKey, String severity) {
        try {
            logger.info("获取项目问题列表: {}, 严重程度: {}", projectKey, severity);
            
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format("%s/api/issues/search?componentKeys=%s&ps=500",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString())));
            
            if (severity != null && !severity.isEmpty()) {
                urlBuilder.append("&severities=").append(URLEncoder.encode(severity, StandardCharsets.UTF_8.toString()));
            }
            
            Optional<JsonNode> response = executeHttpRequest(urlBuilder.toString());
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode responseNode = response.get();
                result.put("total", responseNode.get("total").asInt());
                result.put("issues", responseNode.get("issues"));
                logger.debug("成功获取项目 {} 的问题列表，共 {} 个问题", projectKey, result.get("total"));
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 问题列表失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的安全漏洞
     */
    public Optional<Map<String, Object>> getProjectVulnerabilities(String projectKey) {
        try {
            logger.info("获取项目安全漏洞: {}", projectKey);
            
            String url = String.format("%s/api/issues/search?componentKeys=%s&types=VULNERABILITY&ps=500",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode responseNode = response.get();
                result.put("total", responseNode.get("total").asInt());
                result.put("issues", responseNode.get("issues"));
                logger.debug("成功获取项目 {} 的安全漏洞，共 {} 个", projectKey, result.get("total"));
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 安全漏洞失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的代码异味
     */
    public Optional<Map<String, Object>> getProjectCodeSmells(String projectKey) {
        try {
            logger.info("获取项目代码异味: {}", projectKey);
            
            String url = String.format("%s/api/issues/search?componentKeys=%s&types=CODE_SMELL&ps=500",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode responseNode = response.get();
                result.put("total", responseNode.get("total").asInt());
                result.put("issues", responseNode.get("issues"));
                logger.debug("成功获取项目 {} 的代码异味，共 {} 个", projectKey, result.get("total"));
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 代码异味失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的Bug
     */
    public Optional<Map<String, Object>> getProjectBugs(String projectKey) {
        try {
            logger.info("获取项目Bug: {}", projectKey);
            
            String url = String.format("%s/api/issues/search?componentKeys=%s&types=BUG&ps=500",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode responseNode = response.get();
                result.put("total", responseNode.get("total").asInt());
                result.put("issues", responseNode.get("issues"));
                logger.debug("成功获取项目 {} 的Bug，共 {} 个", projectKey, result.get("total"));
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} Bug失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取项目的最新分析记录
     */
    public Optional<Map<String, Object>> getProjectAnalyses(String projectKey) {
        try {
            logger.info("获取项目分析记录: {}", projectKey);
            
            String url = String.format("%s/api/project_analyses/search?project=%s&ps=10",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                JsonNode responseNode = response.get();
                result.put("analyses", responseNode.get("analyses"));
                logger.debug("成功获取项目 {} 的分析记录", projectKey);
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取项目 {} 分析记录失败: {}", projectKey, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 检查项目是否存在
     */
    public boolean projectExists(String projectKey) {
        try {
            logger.debug("检查项目是否存在: {}", projectKey);
            
            String url = String.format("%s/api/components/search?q=%s&qualifiers=TRK",
                sonarQubeProperties.getUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.toString()));
            
            Optional<JsonNode> response = executeHttpRequest(url);
            if (response.isPresent()) {
                JsonNode components = response.get().get("components");
                if (components != null && components.isArray()) {
                    for (JsonNode component : components) {
                        if (projectKey.equals(component.get("key").asText())) {
                            logger.debug("项目 {} 存在状态: true", projectKey);
                            return true;
                        }
                    }
                }
            }
            
            logger.debug("项目 {} 存在状态: false", projectKey);
            return false;
            
        } catch (Exception e) {
            logger.error("检查项目 {} 是否存在失败: {}", projectKey, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 测试SonarQube连接
     */
    public boolean testConnection() {
        try {
            logger.info("测试SonarQube连接");
            
            String url = sonarQubeProperties.getUrl() + "/api/system/status";
            Optional<JsonNode> response = executeHttpRequest(url);
            
            if (response.isPresent()) {
                logger.info("SonarQube连接测试成功");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("SonarQube连接测试失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 执行HTTP请求
     */
    private Optional<JsonNode> executeHttpRequest(String url) {
        try {
            HttpGet httpGet = new HttpGet(url);
            
            // 如果配置了token，则使用Basic认证（token作为用户名，密码为空）
            if (sonarQubeProperties.getToken() != null && !sonarQubeProperties.getToken().isEmpty()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(sonarQubeProperties.getToken(), "")
                );
                httpGet.setHeader("Authorization", 
                    "Basic " + java.util.Base64.getEncoder().encodeToString(
                        (sonarQubeProperties.getToken() + ":").getBytes(StandardCharsets.UTF_8)
                    )
                );
            }
            
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return Optional.of(objectMapper.readTree(responseBody));
            } else {
                logger.warn("HTTP请求失败: {} - {}", statusCode, response.getStatusLine().getReasonPhrase());
                return Optional.empty();
            }
            
        } catch (IOException e) {
            logger.error("执行HTTP请求失败: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}