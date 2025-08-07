package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.CodeReview;
import com.gitlab.metrics.entity.MergeRequest;
import com.gitlab.metrics.repository.CodeReviewRepository;
import com.gitlab.metrics.repository.MergeRequestRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 评审规则引擎测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class ReviewRuleEngineTest {
    
    @Mock
    private MergeRequestRepository mergeRequestRepository;
    
    @Mock
    private CodeReviewRepository codeReviewRepository;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private ReviewRuleEngine reviewRuleEngine;
    
    private MergeRequest testMergeRequest;
    private CodeReview testCodeReview;
    
    @Before
    public void setUp() {
        // 设置配置参数
        ReflectionTestUtils.setField(reviewRuleEngine, "protectedBranches", "main,master,develop");
        ReflectionTestUtils.setField(reviewRuleEngine, "minReviewers", 1);
        ReflectionTestUtils.setField(reviewRuleEngine, "requireApproval", true);
        ReflectionTestUtils.setField(reviewRuleEngine, "blockSelfApproval", true);
        ReflectionTestUtils.setField(reviewRuleEngine, "adminUsers", "admin1,admin2");
        ReflectionTestUtils.setField(reviewRuleEngine, "emergencyBypassEnabled", true);
        ReflectionTestUtils.setField(reviewRuleEngine, "largeMrThreshold", 500);
        ReflectionTestUtils.setField(reviewRuleEngine, "largeMrMinReviewers", 2);
        
        // 设置测试数据
        testMergeRequest = createTestMergeRequest();
        testCodeReview = createTestCodeReview();
    }
    
    @Test
    public void testCanMerge_Success() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(testCodeReview));
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertTrue(result.isCanMerge());
        assertEquals("123", result.getMrId());
        assertNotNull(result.getCheckedAt());
    }
    
    @Test
    public void testCanMerge_InsufficientReviewers() {
        // Given
        testMergeRequest.setTargetBranch("main");
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Collections.emptyList());
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertFalse(result.isCanMerge());
        assertEquals(2, result.getViolations().size()); // Both insufficient reviewers and missing approvals
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Insufficient reviewers".equals(v.getRule())));
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Missing required approvals".equals(v.getRule())));
    }
    
    @Test
    public void testCanMerge_MissingApproval() {
        // Given
        testMergeRequest.setTargetBranch("main");
        CodeReview commentedReview = new CodeReview();
        commentedReview.setReviewerId("reviewer1");
        commentedReview.setStatus("commented");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(commentedReview));
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertFalse(result.isCanMerge());
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Missing required approvals".equals(v.getRule())));
    }
    
    @Test
    public void testCanMerge_UnresolvedChangeRequests() {
        // Given
        testMergeRequest.setTargetBranch("main");
        CodeReview changesRequestedReview = new CodeReview();
        changesRequestedReview.setReviewerId("reviewer1");
        changesRequestedReview.setStatus("changes_requested");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(changesRequestedReview));
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertFalse(result.isCanMerge());
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Unresolved change requests".equals(v.getRule())));
    }
    
    @Test
    public void testCanMerge_SelfApproval() {
        // Given
        testMergeRequest.setTargetBranch("main");
        testMergeRequest.setAuthorId("author1");
        
        CodeReview selfApprovalReview = new CodeReview();
        selfApprovalReview.setReviewerId("author1"); // Same as author
        selfApprovalReview.setStatus("approved");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(selfApprovalReview));
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertFalse(result.isCanMerge());
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Self-approval not allowed".equals(v.getRule())));
    }
    
    @Test
    public void testCanMerge_ReviewNotRequired() {
        // Given
        testMergeRequest.setTargetBranch("feature/test"); // Not a protected branch
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertTrue(result.isCanMerge());
        assertTrue(result.getMessages().contains("Review not required for this branch"));
    }
    
    @Test
    public void testCanMerge_LargeMR() {
        // Given
        testMergeRequest.setTargetBranch("main");
        testMergeRequest.setAdditions(300);
        testMergeRequest.setDeletions(300); // Total 600 > 500 threshold
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(testCodeReview)); // Only 1 reviewer, but needs 2
        
        // When
        ReviewRuleEngine.ReviewRuleResult result = reviewRuleEngine.canMerge("123");
        
        // Then
        assertFalse(result.isCanMerge());
        assertTrue(result.getViolations().stream()
            .anyMatch(v -> "Insufficient reviewers".equals(v.getRule()) && 
                          v.getDescription().contains("at least 2 reviewers")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCanMerge_MergeRequestNotFound() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.empty());
        
        // When
        reviewRuleEngine.canMerge("123");
        
        // Then - exception should be thrown
    }
    
    @Test
    public void testAuthorizeEmergencyBypass_Success() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        
        // When
        ReviewRuleEngine.EmergencyBypassResult result = reviewRuleEngine.authorizeEmergencyBypass(
            "123", "admin1", "Critical production fix");
        
        // Then
        assertTrue(result.isAuthorized());
        assertEquals("123", result.getMrId());
        assertNotNull(result.getBypass());
        assertEquals("admin1", result.getBypass().getAuthorizedBy());
        assertEquals("Critical production fix", result.getBypass().getReason());
        verify(alertService).sendAlert(any(AlertService.Alert.class));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAuthorizeEmergencyBypass_UnauthorizedUser() {
        // Given - no mocking needed as exception is thrown before repository call
        
        // When
        reviewRuleEngine.authorizeEmergencyBypass("123", "regular_user", "Some reason");
        
        // Then - exception should be thrown
    }
    
    @Test(expected = IllegalStateException.class)
    public void testAuthorizeEmergencyBypass_Disabled() {
        // Given
        ReflectionTestUtils.setField(reviewRuleEngine, "emergencyBypassEnabled", false);
        // No mocking needed as exception is thrown before repository call
        
        // When
        reviewRuleEngine.authorizeEmergencyBypass("123", "admin1", "Some reason");
        
        // Then - exception should be thrown
    }
    
    @Test
    public void testCalculateReviewCoverage() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        
        MergeRequest mr1 = createTestMergeRequest();
        mr1.setId(1L);
        mr1.setCreatedAt(start.plusDays(1));
        
        MergeRequest mr2 = createTestMergeRequest();
        mr2.setId(2L);
        mr2.setCreatedAt(start.plusDays(2));
        
        CodeReview review1 = new CodeReview();
        review1.setReviewerId("reviewer1");
        review1.setStatus("approved");
        review1.setReviewedAt(start.plusDays(1).plusHours(2));
        
        when(mergeRequestRepository.findByProjectIdAndCreatedAtBetween("project1", start, end))
            .thenReturn(Arrays.asList(mr1, mr2));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(review1));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(2L))
            .thenReturn(Collections.emptyList());
        
        // When
        ReviewRuleEngine.ReviewCoverageStats stats = reviewRuleEngine.calculateReviewCoverage(
            "project1", start, end);
        
        // Then
        assertEquals("project1", stats.getProjectId());
        assertEquals(Integer.valueOf(2), stats.getTotalMergeRequests());
        assertEquals(Integer.valueOf(1), stats.getReviewedMergeRequests());
        assertEquals(Integer.valueOf(1), stats.getApprovedMergeRequests());
        assertEquals(Integer.valueOf(0), stats.getRejectedMergeRequests());
        assertEquals(Double.valueOf(50.0), stats.getReviewCoverageRate());
        assertEquals(Double.valueOf(50.0), stats.getApprovalRate());
        assertEquals(Double.valueOf(2.0), stats.getAverageReviewTimeHours());
    }
    
    // Helper methods to create test data
    private MergeRequest createTestMergeRequest() {
        MergeRequest mr = new MergeRequest();
        mr.setId(1L);
        mr.setMrId("123");
        mr.setProjectId("project1");
        mr.setAuthorId("author1");
        mr.setAuthorName("Test Author");
        mr.setTitle("Test MR");
        mr.setSourceBranch("feature/test");
        mr.setTargetBranch("main");
        mr.setStatus("opened");
        mr.setCreatedAt(LocalDateTime.now().minusHours(1));
        mr.setAdditions(100);
        mr.setDeletions(50);
        return mr;
    }
    
    private CodeReview createTestCodeReview() {
        CodeReview review = new CodeReview();
        review.setId(1L);
        review.setReviewerId("reviewer1");
        review.setReviewerName("Test Reviewer");
        review.setReviewedAt(LocalDateTime.now());
        review.setStatus("approved");
        review.setComment("Looks good!");
        return review;
    }
}