package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.config.RabbitMQConfig;
import com.gitlab.metrics.service.AlertService.Alert;
import com.gitlab.metrics.service.AlertService.AlertLevel;
import com.gitlab.metrics.service.AlertService.AlertType;
import com.gitlab.metrics.service.SecurityAnalysisService.QualityThresholdResult;
import com.gitlab.metrics.service.SecurityAnalysisService.SecurityAnalysisResult;
import com.gitlab.metrics.service.SecurityAnalysisService.PerformanceAnalysisResult;
import com.gitlab.metrics.service.SecurityAnalysisService.ThresholdViolation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlertService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AlertService alertService;

    private SecurityAnalysisResult securityResult;
    private PerformanceAnalysisResult performanceResult;
    private QualityThresholdResult qualityResult;

    @Before
    public void setUp() {
        // 准备安全分析结果测试数据
        securityResult = new SecurityAnalysisResult();
        securityResult.setProjectId("test-project");
        securityResult.setTotalVulnerabilities(5);
        securityResult.setRiskScore(15.0);

        // 准备性能分析结果测试数据
        performanceResult = new PerformanceAnalysisResult();
        performanceResult.setProjectId("test-project");
        performanceResult.setTotalIssues(12);
        performanceResult.setComplexityScore(18.0);
        performanceResult.setRiskScore(25.0);

        // 准备质量阈值结果测试数据
        qualityResult = new QualityThresholdResult();
        qualityResult.setProjectId("test-project");
        qualityResult.setShouldBlockMerge(true);
        
        ThresholdViolation securityViolation = new ThresholdViolation();
        securityViolation.setDescription("安全漏洞数量超标");
        securityViolation.setActualValue("10");
        securityViolation.setExpectedValue("5");
        securityViolation.setSeverity("HIGH");
        
        qualityResult.setSecurityViolations(Arrays.asList(securityViolation));
        qualityResult.setPerformanceViolations(Collections.emptyList());
        qualityResult.setQualityGateViolations(Collections.emptyList());
    }

    @Test
    public void testHandleSecurityAnalysisAlert_WithVulnerabilities() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenReturn("{\"test\":\"alert\"}");

        // When
        alertService.handleSecurityAnalysisAlert(securityResult);

        // Then
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
            exchangeCaptor.capture(),
            routingKeyCaptor.capture(),
            messageCaptor.capture()
        );

        assertEquals(RabbitMQConfig.GITLAB_EVENTS_EXCHANGE, exchangeCaptor.getValue());
        assertEquals("alert.notification", routingKeyCaptor.getValue());
        assertEquals("{\"test\":\"alert\"}", messageCaptor.getValue());
    }

    @Test
    public void testHandleSecurityAnalysisAlert_NoVulnerabilities() throws Exception {
        // Given
        securityResult.setTotalVulnerabilities(0);

        // When
        alertService.handleSecurityAnalysisAlert(securityResult);

        // Then
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    public void testHandlePerformanceAnalysisAlert_WithIssues() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenReturn("{\"test\":\"alert\"}");

        // When
        alertService.handlePerformanceAnalysisAlert(performanceResult);

        // Then
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.GITLAB_EVENTS_EXCHANGE),
            eq("alert.notification"),
            eq("{\"test\":\"alert\"}")
        );
    }

    @Test
    public void testHandlePerformanceAnalysisAlert_LowIssues() throws Exception {
        // Given
        performanceResult.setTotalIssues(5);
        performanceResult.setComplexityScore(10.0);

        // When
        alertService.handlePerformanceAnalysisAlert(performanceResult);

        // Then
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    public void testHandleQualityThresholdAlert_ShouldBlockMerge() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenReturn("{\"test\":\"alert\"}");

        // When
        alertService.handleQualityThresholdAlert(qualityResult);

        // Then
        // 应该发送2个告警：1个合并阻止告警 + 1个安全违规告警
        verify(rabbitTemplate, times(2)).convertAndSend(
            eq(RabbitMQConfig.GITLAB_EVENTS_EXCHANGE),
            eq("alert.notification"),
            eq("{\"test\":\"alert\"}")
        );
    }

    @Test
    public void testSendAlert() throws Exception {
        // Given
        Alert alert = new Alert();
        alert.setProjectId("test-project");
        alert.setType(AlertType.SECURITY_VULNERABILITY);
        alert.setLevel(AlertLevel.HIGH);
        alert.setTitle("测试告警");
        alert.setMessage("这是一个测试告警");
        alert.setTimestamp(LocalDateTime.now());

        when(objectMapper.writeValueAsString(alert)).thenReturn("{\"test\":\"alert\"}");

        // When
        alertService.sendAlert(alert);

        // Then
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.GITLAB_EVENTS_EXCHANGE),
            eq("alert.notification"),
            eq("{\"test\":\"alert\"}")
        );
    }

    @Test
    public void testSendCoverageAlert() throws Exception {
        // Given
        String projectId = "test-project";
        String commitSha = "abc123";
        String alertType = "COVERAGE_LOW";
        String message = "覆盖率低于阈值";

        when(objectMapper.writeValueAsString(any(Alert.class))).thenReturn("{\"test\":\"alert\"}");

        // When
        alertService.sendCoverageAlert(projectId, commitSha, alertType, message);

        // Then
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(objectMapper).writeValueAsString(alertCaptor.capture());

        Alert capturedAlert = alertCaptor.getValue();
        assertEquals(projectId, capturedAlert.getProjectId());
        assertEquals(commitSha, capturedAlert.getRelatedEntityId());
        assertEquals(AlertType.QUALITY_GATE_FAILURE, capturedAlert.getType());
        assertEquals(AlertLevel.HIGH, capturedAlert.getLevel());
        assertEquals("覆盖率质量门禁告警", capturedAlert.getTitle());
        assertEquals(message, capturedAlert.getMessage());
        assertNotNull(capturedAlert.getTimestamp());
    }

    @Test
    public void testAlertLevelEnum() {
        // Test AlertLevel enum
        assertEquals("CRITICAL", AlertLevel.CRITICAL.getLevel());
        assertEquals(1, AlertLevel.CRITICAL.getPriority());
        assertEquals("HIGH", AlertLevel.HIGH.getLevel());
        assertEquals(2, AlertLevel.HIGH.getPriority());
        assertEquals("MEDIUM", AlertLevel.MEDIUM.getLevel());
        assertEquals(3, AlertLevel.MEDIUM.getPriority());
        assertEquals("LOW", AlertLevel.LOW.getLevel());
        assertEquals(4, AlertLevel.LOW.getPriority());
        assertEquals("INFO", AlertLevel.INFO.getLevel());
        assertEquals(5, AlertLevel.INFO.getPriority());
    }

    @Test
    public void testAlertTypeEnum() {
        // Test AlertType enum
        assertEquals("安全漏洞", AlertType.SECURITY_VULNERABILITY.getDescription());
        assertEquals("性能问题", AlertType.PERFORMANCE_ISSUE.getDescription());
        assertEquals("质量门禁失败", AlertType.QUALITY_GATE_FAILURE.getDescription());
        assertEquals("阈值违规", AlertType.THRESHOLD_VIOLATION.getDescription());
        assertEquals("合并阻止", AlertType.MERGE_BLOCKED.getDescription());
    }

    @Test
    public void testAlertClass() {
        // Test Alert class
        Alert alert = new Alert();
        alert.setProjectId("test-project");
        alert.setAssigneeId("user123");
        alert.setRelatedEntityId("entity456");
        alert.setIssueId("issue789");
        alert.setType(AlertType.SECURITY_VULNERABILITY);
        alert.setLevel(AlertLevel.HIGH);
        alert.setTitle("测试标题");
        alert.setMessage("测试消息");
        
        LocalDateTime now = LocalDateTime.now();
        alert.setTimestamp(now);
        alert.setCreatedAt(now);
        alert.setDetails("测试详情");

        assertEquals("test-project", alert.getProjectId());
        assertEquals("user123", alert.getAssigneeId());
        assertEquals("entity456", alert.getRelatedEntityId());
        assertEquals("issue789", alert.getIssueId());
        assertEquals(AlertType.SECURITY_VULNERABILITY, alert.getType());
        assertEquals(AlertLevel.HIGH, alert.getLevel());
        assertEquals("测试标题", alert.getTitle());
        assertEquals("测试消息", alert.getMessage());
        assertEquals(now, alert.getTimestamp());
        assertEquals(now, alert.getCreatedAt());
        assertEquals("测试详情", alert.getDetails());

        // Test toString method
        String alertString = alert.toString();
        assertTrue(alertString.contains("test-project"));
        assertTrue(alertString.contains("user123"));
        assertTrue(alertString.contains("entity456"));
        assertTrue(alertString.contains("SECURITY_VULNERABILITY"));
        assertTrue(alertString.contains("HIGH"));
    }

    @Test
    public void testHandleSecurityAnalysisAlert_ExceptionHandling() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenThrow(new RuntimeException("JSON序列化失败"));

        // When
        alertService.handleSecurityAnalysisAlert(securityResult);

        // Then - 应该不抛出异常，只记录日志
        verify(objectMapper).writeValueAsString(any(Alert.class));
    }

    @Test
    public void testHandlePerformanceAnalysisAlert_ExceptionHandling() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenThrow(new RuntimeException("JSON序列化失败"));

        // When
        alertService.handlePerformanceAnalysisAlert(performanceResult);

        // Then - 应该不抛出异常，只记录日志
        verify(objectMapper).writeValueAsString(any(Alert.class));
    }

    @Test
    public void testHandleQualityThresholdAlert_ExceptionHandling() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(Alert.class))).thenThrow(new RuntimeException("JSON序列化失败"));

        // When
        alertService.handleQualityThresholdAlert(qualityResult);

        // Then - 应该不抛出异常，只记录日志
        verify(objectMapper, atLeastOnce()).writeValueAsString(any(Alert.class));
    }
}