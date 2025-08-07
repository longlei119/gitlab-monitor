package com.gitlab.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.metrics.service.CoverageQualityGateService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CoverageQualityGateController单元测试
 */
@RunWith(SpringRunner.class)
@WebMvcTest(CoverageQualityGateController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithMockUser
public class CoverageQualityGateControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CoverageQualityGateService qualityGateService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String projectId;
    private String commitSha;
    
    @Before
    public void setUp() {
        projectId = "test-project";
        commitSha = "abc123def456";
    }
    
    @Test
    public void testCheckQualityGatePass() throws Exception {
        // 准备测试数据
        CoverageQualityGateService.QualityGateResult result = 
            new CoverageQualityGateService.QualityGateResult(true, "质量门禁检查通过", Arrays.asList());
        
        when(qualityGateService.checkQualityGate(projectId, commitSha)).thenReturn(result);
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check")
                .param("projectId", projectId)
                .param("commitSha", commitSha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.message").value("质量门禁检查通过"))
                .andExpect(jsonPath("$.violations").isEmpty());
    }
    
    @Test
    public void testCheckQualityGateFail() throws Exception {
        // 准备测试数据
        List<String> violations = Arrays.asList("行覆盖率 70.00% 低于阈值 80.00%");
        CoverageQualityGateService.QualityGateResult result = 
            new CoverageQualityGateService.QualityGateResult(false, "质量门禁检查失败", violations);
        
        when(qualityGateService.checkQualityGate(projectId, commitSha)).thenReturn(result);
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check")
                .param("projectId", projectId)
                .param("commitSha", commitSha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.message").value("质量门禁检查失败"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations[0]").value("行覆盖率 70.00% 低于阈值 80.00%"));
    }
    
    @Test
    public void testCheckQualityGateWithCustomThreshold() throws Exception {
        // 准备测试数据
        CoverageQualityGateService.QualityGateResult result = 
            new CoverageQualityGateService.QualityGateResult(true, "质量门禁检查通过", Arrays.asList());
        
        when(qualityGateService.checkQualityGate(eq(projectId), eq(commitSha), eq(90.0), eq(80.0), eq(85.0)))
            .thenReturn(result);
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check/custom")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .param("lineThreshold", "90.0")
                .param("branchThreshold", "80.0")
                .param("functionThreshold", "85.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.message").value("质量门禁检查通过"));
    }
    
    @Test
    public void testCheckNewCodeTestRequirement() throws Exception {
        // 准备测试数据
        CoverageQualityGateService.NewCodeTestResult result = 
            new CoverageQualityGateService.NewCodeTestResult(true, "新增代码覆盖率达到要求", 20, 18);
        
        when(qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 20)).thenReturn(result);
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check/new-code")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .param("newCodeLines", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.message").value("新增代码覆盖率达到要求"))
                .andExpect(jsonPath("$.newCodeLines").value(20))
                .andExpect(jsonPath("$.newCoveredLines").value(18));
    }
    
    @Test
    public void testCheckTestFailures() throws Exception {
        // 准备测试数据
        CoverageQualityGateService.TestResults testResults = 
            new CoverageQualityGateService.TestResults(100, 95, 5, 0);
        
        CoverageQualityGateService.TestFailureResult result = 
            new CoverageQualityGateService.TestFailureResult(false, "存在 5 个失败的测试用例", false);
        
        when(qualityGateService.checkTestFailures(eq(projectId), eq(commitSha), any(CoverageQualityGateService.TestResults.class)))
            .thenReturn(result);
        
        // 执行测试
        mockMvc.perform(post("/api/coverage/quality-gate/check/test-failures")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testResults))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.message").value("存在 5 个失败的测试用例"))
                .andExpect(jsonPath("$.deploymentBlocked").value(false));
    }
    
    @Test
    public void testBlockDeployment() throws Exception {
        String reason = "覆盖率不达标";
        
        // 执行测试
        mockMvc.perform(post("/api/coverage/quality-gate/block-deployment")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .param("reason", reason)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("部署已被阻止"));
    }
    
    @Test
    public void testGetQualityGateStats() throws Exception {
        // 准备测试数据
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2023, 1, 31, 23, 59);
        
        CoverageQualityGateService.QualityGateStats stats = 
            new CoverageQualityGateService.QualityGateStats(projectId, start, end, 80L, 20L, 100L);
        
        when(qualityGateService.getQualityGateStats(eq(projectId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(stats);
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/stats")
                .param("projectId", projectId)
                .param("startDate", "2023-01-01T00:00:00")
                .param("endDate", "2023-01-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.passedCount").value(80))
                .andExpect(jsonPath("$.failedCount").value(20))
                .andExpect(jsonPath("$.totalCount").value(100))
                .andExpect(jsonPath("$.passRate").value(80.0));
    }
    
    @Test
    public void testCheckQualityGateException() throws Exception {
        // 准备测试数据 - 抛出异常
        when(qualityGateService.checkQualityGate(projectId, commitSha))
            .thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check")
                .param("projectId", projectId)
                .param("commitSha", commitSha))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    public void testCheckNewCodeTestRequirementException() throws Exception {
        // 准备测试数据 - 抛出异常
        when(qualityGateService.checkNewCodeTestRequirement(projectId, commitSha, 20))
            .thenThrow(new RuntimeException("计算失败"));
        
        // 执行测试
        mockMvc.perform(get("/api/coverage/quality-gate/check/new-code")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .param("newCodeLines", "20"))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    public void testBlockDeploymentException() throws Exception {
        String reason = "覆盖率不达标";
        
        // 准备测试数据 - 抛出异常
        doThrow(new RuntimeException("告警服务不可用"))
            .when(qualityGateService).blockDeployment(projectId, commitSha, reason);
        
        // 执行测试
        mockMvc.perform(post("/api/coverage/quality-gate/block-deployment")
                .param("projectId", projectId)
                .param("commitSha", commitSha)
                .param("reason", reason)
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("阻止部署失败"));
    }
}