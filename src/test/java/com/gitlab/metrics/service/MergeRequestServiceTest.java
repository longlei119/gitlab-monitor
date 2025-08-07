package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.MergeRequestEventRequest;
import com.gitlab.metrics.dto.webhook.WebhookRequest;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 合并请求服务测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class MergeRequestServiceTest {
    
    @Mock
    private MergeRequestRepository mergeRequestRepository;
    
    @Mock
    private CodeReviewRepository codeReviewRepository;
    
    @InjectMocks
    private MergeRequestService mergeRequestService;
    
    private MergeRequestEventRequest testEvent;
    private MergeRequest testMergeRequest;
    private CodeReview testCodeReview;
    
    @Before
    public void setUp() {
        // 设置测试数据
        testEvent = createTestMergeRequestEvent();
        testMergeRequest = createTestMergeRequest();
        testCodeReview = createTestCodeReview();
    }
    
    @Test
    public void testProcessMergeRequestEvent_NewMergeRequest() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.empty());
        when(mergeRequestRepository.save(any(MergeRequest.class))).thenReturn(testMergeRequest);
        
        // When
        mergeRequestService.processMergeRequestEvent(testEvent);
        
        // Then
        verify(mergeRequestRepository).findByMrId("123");
        verify(mergeRequestRepository).save(any(MergeRequest.class));
    }
    
    @Test
    public void testProcessMergeRequestEvent_ExistingMergeRequest() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(mergeRequestRepository.save(any(MergeRequest.class))).thenReturn(testMergeRequest);
        
        // When
        mergeRequestService.processMergeRequestEvent(testEvent);
        
        // Then
        verify(mergeRequestRepository).findByMrId("123");
        verify(mergeRequestRepository).save(any(MergeRequest.class));
    }
    
    @Test
    public void testAddCodeReview_Success() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(testCodeReview);
        
        // When
        CodeReview result = mergeRequestService.addCodeReview("123", "reviewer1", "John Reviewer", 
                                                             "approved", "Looks good!");
        
        // Then
        assertNotNull(result);
        verify(mergeRequestRepository).findByMrId("123");
        verify(codeReviewRepository).save(any(CodeReview.class));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddCodeReview_MergeRequestNotFound() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.empty());
        
        // When
        mergeRequestService.addCodeReview("123", "reviewer1", "John Reviewer", 
                                         "approved", "Looks good!");
        
        // Then - exception should be thrown
    }
    
    @Test
    public void testIsReviewRequired_MainBranch() {
        // Given
        testMergeRequest.setTargetBranch("main");
        
        // When
        boolean result = mergeRequestService.isReviewRequired(testMergeRequest);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    public void testIsReviewRequired_MasterBranch() {
        // Given
        testMergeRequest.setTargetBranch("master");
        
        // When
        boolean result = mergeRequestService.isReviewRequired(testMergeRequest);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    public void testIsReviewRequired_DevelopBranch() {
        // Given
        testMergeRequest.setTargetBranch("develop");
        
        // When
        boolean result = mergeRequestService.isReviewRequired(testMergeRequest);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    public void testIsReviewRequired_FeatureBranch() {
        // Given
        testMergeRequest.setTargetBranch("feature/test");
        
        // When
        boolean result = mergeRequestService.isReviewRequired(testMergeRequest);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    public void testIsReviewApprovalSatisfied_WithApproval() {
        // Given
        CodeReview approvedReview = new CodeReview();
        approvedReview.setStatus("approved");
        
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(approvedReview));
        
        // When
        boolean result = mergeRequestService.isReviewApprovalSatisfied(testMergeRequest);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    public void testIsReviewApprovalSatisfied_WithChangesRequested() {
        // Given
        CodeReview changesRequestedReview = new CodeReview();
        changesRequestedReview.setStatus("changes_requested");
        
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(changesRequestedReview));
        
        // When
        boolean result = mergeRequestService.isReviewApprovalSatisfied(testMergeRequest);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    public void testIsReviewApprovalSatisfied_NoReviews_ReviewRequired() {
        // Given
        testMergeRequest.setTargetBranch("main"); // Review required
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Collections.emptyList());
        
        // When
        boolean result = mergeRequestService.isReviewApprovalSatisfied(testMergeRequest);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    public void testIsReviewApprovalSatisfied_NoReviews_ReviewNotRequired() {
        // Given
        testMergeRequest.setTargetBranch("feature/test"); // Review not required
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Collections.emptyList());
        
        // When
        boolean result = mergeRequestService.isReviewApprovalSatisfied(testMergeRequest);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    public void testGetReviewStatus_NotFound() {
        // Given
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.empty());
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("not_found", result);
    }
    
    @Test
    public void testGetReviewStatus_NotRequired() {
        // Given
        testMergeRequest.setTargetBranch("feature/test");
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("not_required", result);
    }
    
    @Test
    public void testGetReviewStatus_Approved() {
        // Given
        testMergeRequest.setTargetBranch("main");
        CodeReview approvedReview = new CodeReview();
        approvedReview.setStatus("approved");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(approvedReview));
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("approved", result);
    }
    
    @Test
    public void testGetReviewStatus_Pending() {
        // Given
        testMergeRequest.setTargetBranch("main");
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Collections.emptyList());
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("pending", result);
    }
    
    @Test
    public void testGetReviewStatus_ChangesRequested() {
        // Given
        testMergeRequest.setTargetBranch("main");
        CodeReview changesRequestedReview = new CodeReview();
        changesRequestedReview.setStatus("changes_requested");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(changesRequestedReview));
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("changes_requested", result);
    }
    
    @Test
    public void testGetReviewStatus_InReview() {
        // Given
        testMergeRequest.setTargetBranch("main");
        CodeReview commentedReview = new CodeReview();
        commentedReview.setStatus("commented");
        
        when(mergeRequestRepository.findByMrId("123")).thenReturn(Optional.of(testMergeRequest));
        when(codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(1L))
            .thenReturn(Arrays.asList(commentedReview));
        
        // When
        String result = mergeRequestService.getReviewStatus("123");
        
        // Then
        assertEquals("in_review", result);
    }
    
    // Helper methods to create test data
    private MergeRequestEventRequest createTestMergeRequestEvent() {
        MergeRequestEventRequest event = new MergeRequestEventRequest();
        
        MergeRequestEventRequest.MergeRequestAttributes attrs = new MergeRequestEventRequest.MergeRequestAttributes();
        attrs.setId(123L);
        attrs.setTargetProjectId(456L);
        attrs.setAuthorId(789L);
        attrs.setTitle("Test MR");
        attrs.setDescription("Test description");
        attrs.setSourceBranch("feature/test");
        attrs.setTargetBranch("main");
        attrs.setState("opened");
        attrs.setAction("open");
        attrs.setCreatedAt("2023-01-01T10:00:00.000Z");
        attrs.setUpdatedAt("2023-01-01T10:00:00.000Z");
        
        event.setObjectAttributes(attrs);
        
        WebhookRequest.UserInfo user = new WebhookRequest.UserInfo();
        user.setId(789L);
        user.setName("Test Author");
        event.setUser(user);
        
        return event;
    }
    
    private MergeRequest createTestMergeRequest() {
        MergeRequest mr = new MergeRequest();
        mr.setId(1L);
        mr.setMrId("123");
        mr.setProjectId("456");
        mr.setAuthorId("789");
        mr.setAuthorName("Test Author");
        mr.setTitle("Test MR");
        mr.setDescription("Test description");
        mr.setSourceBranch("feature/test");
        mr.setTargetBranch("main");
        mr.setStatus("opened");
        mr.setCreatedAt(LocalDateTime.now());
        mr.setUpdatedAt(LocalDateTime.now());
        return mr;
    }
    
    private CodeReview createTestCodeReview() {
        CodeReview review = new CodeReview();
        review.setId(1L);
        review.setMergeRequest(testMergeRequest);
        review.setReviewerId("reviewer1");
        review.setReviewerName("John Reviewer");
        review.setReviewedAt(LocalDateTime.now());
        review.setStatus("approved");
        review.setComment("Looks good!");
        review.setReviewType("manual");
        review.setIsRequired(true);
        return review;
    }
}