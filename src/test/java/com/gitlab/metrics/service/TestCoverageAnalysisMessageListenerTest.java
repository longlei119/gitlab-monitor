package com.gitlab.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.entity.TestCoverage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TestCoverageAnalysisMessageListener单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class TestCoverageAnalysisMessageListenerTest {
    
    @Mock
    private TestCoverageService testCoverageService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private TestCoverageAnalysisMessageListener messageListener;
    
    private String projectId;
    private String commitSha;
    private String reportContent;
    private String reportType;
    private String reportPath;
    private String filePath;
    
    @Before
    public void setUp() {
        projectId = "test-project";
        commitSha = "abc123def456";
        reportContent = "<?xml version=\"1.0\"?><report></report>";
        reportType = "jacoco";
        reportPath = "/path/to/report.xml";
        filePath = "/path/to/coverage.xml";
    }
    
    @Test
    public void testHandleCoverageAnalysis() throws Exception {
        // 准备测试数据
        TestCoverageAnalysisMessageListener.CoverageAnalysisMessage message = 
            new TestCoverageAnalysisMessageListener.CoverageAnalysisMessage(
                projectId, commitSha, reportContent, reportType, reportPath);
        
        String messageJson = "{\"projectId\":\"" + projectId + "\",\"commitSha\":\"" + commitSha + 
                           "\",\"reportContent\":\"" + reportContent + "\",\"reportType\":\"" + reportType + 
                           "\",\"reportPath\":\"" + reportPath + "\"}";
        
        TestCoverage mockCoverage = new TestCoverage(projectId, commitSha, LocalDateTime.now());
        mockCoverage.setId(1L);
        
        when(objectMapper.readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageAnalysisMessage.class))
            .thenReturn(message);
        when(testCoverageService.parseCoverageReport(projectId, commitSha, reportContent, reportType, reportPath))
            .thenReturn(mockCoverage);
        
        // 执行测试
        messageListener.handleCoverageAnalysis(messageJson);
        
        // 验证结果
        verify(objectMapper).readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageAnalysisMessage.class);
        verify(testCoverageService).parseCoverageReport(projectId, commitSha, reportContent, reportType, reportPath);
    }
    
    @Test
    public void testHandleCoverageAnalysisWithException() throws Exception {
        // 准备测试数据
        String messageJson = "{\"invalid\":\"json\"}";
        
        when(objectMapper.readValue(eq(messageJson), eq(TestCoverageAnalysisMessageListener.CoverageAnalysisMessage.class)))
            .thenThrow(new RuntimeException("JSON解析失败"));
        
        // 执行测试（不应该抛出异常）
        messageListener.handleCoverageAnalysis(messageJson);
        
        // 验证结果
        verify(objectMapper).readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageAnalysisMessage.class);
        verify(testCoverageService, never()).parseCoverageReport(any(), any(), any(), any(), any());
    }
    
    @Test
    public void testHandleCoverageFileAnalysis() throws Exception {
        // 准备测试数据
        TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage message = 
            new TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage(
                projectId, commitSha, filePath, reportType);
        
        String messageJson = "{\"projectId\":\"" + projectId + "\",\"commitSha\":\"" + commitSha + 
                           "\",\"filePath\":\"" + filePath + "\",\"reportType\":\"" + reportType + "\"}";
        
        TestCoverage mockCoverage = new TestCoverage(projectId, commitSha, LocalDateTime.now());
        mockCoverage.setId(1L);
        
        when(objectMapper.readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage.class))
            .thenReturn(message);
        when(testCoverageService.parseCoverageReportFromFile(projectId, commitSha, filePath, reportType))
            .thenReturn(mockCoverage);
        
        // 执行测试
        messageListener.handleCoverageFileAnalysis(messageJson);
        
        // 验证结果
        verify(objectMapper).readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage.class);
        verify(testCoverageService).parseCoverageReportFromFile(projectId, commitSha, filePath, reportType);
    }
    
    @Test
    public void testHandleCoverageFileAnalysisWithException() throws Exception {
        // 准备测试数据
        String messageJson = "{\"invalid\":\"json\"}";
        
        when(objectMapper.readValue(eq(messageJson), eq(TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage.class)))
            .thenThrow(new RuntimeException("JSON解析失败"));
        
        // 执行测试（不应该抛出异常）
        messageListener.handleCoverageFileAnalysis(messageJson);
        
        // 验证结果
        verify(objectMapper).readValue(messageJson, TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage.class);
        verify(testCoverageService, never()).parseCoverageReportFromFile(any(), any(), any(), any());
    }
    
    @Test
    public void testCoverageAnalysisMessageConstructors() {
        // 测试默认构造函数
        TestCoverageAnalysisMessageListener.CoverageAnalysisMessage message1 = 
            new TestCoverageAnalysisMessageListener.CoverageAnalysisMessage();
        
        message1.setProjectId(projectId);
        message1.setCommitSha(commitSha);
        message1.setReportContent(reportContent);
        message1.setReportType(reportType);
        message1.setReportPath(reportPath);
        
        assertEquals(projectId, message1.getProjectId());
        assertEquals(commitSha, message1.getCommitSha());
        assertEquals(reportContent, message1.getReportContent());
        assertEquals(reportType, message1.getReportType());
        assertEquals(reportPath, message1.getReportPath());
        
        // 测试带参数构造函数
        TestCoverageAnalysisMessageListener.CoverageAnalysisMessage message2 = 
            new TestCoverageAnalysisMessageListener.CoverageAnalysisMessage(
                projectId, commitSha, reportContent, reportType, reportPath);
        
        assertEquals(projectId, message2.getProjectId());
        assertEquals(commitSha, message2.getCommitSha());
        assertEquals(reportContent, message2.getReportContent());
        assertEquals(reportType, message2.getReportType());
        assertEquals(reportPath, message2.getReportPath());
    }
    
    @Test
    public void testCoverageFileAnalysisMessageConstructors() {
        // 测试默认构造函数
        TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage message1 = 
            new TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage();
        
        message1.setProjectId(projectId);
        message1.setCommitSha(commitSha);
        message1.setFilePath(filePath);
        message1.setReportType(reportType);
        
        assertEquals(projectId, message1.getProjectId());
        assertEquals(commitSha, message1.getCommitSha());
        assertEquals(filePath, message1.getFilePath());
        assertEquals(reportType, message1.getReportType());
        
        // 测试带参数构造函数
        TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage message2 = 
            new TestCoverageAnalysisMessageListener.CoverageFileAnalysisMessage(
                projectId, commitSha, filePath, reportType);
        
        assertEquals(projectId, message2.getProjectId());
        assertEquals(commitSha, message2.getCommitSha());
        assertEquals(filePath, message2.getFilePath());
        assertEquals(reportType, message2.getReportType());
    }
}