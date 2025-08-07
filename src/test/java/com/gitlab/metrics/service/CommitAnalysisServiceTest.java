package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.repository.FileChangeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 代码提交分析服务测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class CommitAnalysisServiceTest {
    
    @Mock
    private CommitRepository commitRepository;
    
    @Mock
    private FileChangeRepository fileChangeRepository;
    
    @InjectMocks
    private CommitAnalysisService commitAnalysisService;
    
    private PushEventRequest pushEvent;
    private PushEventRequest.CommitInfo commitInfo;
    private PushEventRequest.AuthorInfo authorInfo;
    
    @Before
    public void setUp() {
        // 设置测试数据
        pushEvent = new PushEventRequest();
        pushEvent.setProjectId(123L);
        pushEvent.setRef("refs/heads/main");
        pushEvent.setUserName("Test User");
        pushEvent.setUserEmail("test@example.com");
        
        authorInfo = new PushEventRequest.AuthorInfo();
        authorInfo.setName("John Doe");
        authorInfo.setEmail("john.doe@example.com");
        
        commitInfo = new PushEventRequest.CommitInfo();
        commitInfo.setId("abc123def456");
        commitInfo.setMessage("Add new feature");
        commitInfo.setTimestamp("2023-12-01T10:30:00");
        commitInfo.setAuthor(authorInfo);
        commitInfo.setAdded(Arrays.asList("src/main/java/NewClass.java", "src/test/java/NewClassTest.java"));
        commitInfo.setModified(Arrays.asList("src/main/java/ExistingClass.java"));
        commitInfo.setRemoved(Arrays.asList("src/main/java/OldClass.java"));
        
        pushEvent.setCommits(Collections.singletonList(commitInfo));
    }
    
    @Test
    public void testProcessPushEvent_Success() {
        // 模拟数据库中不存在该提交
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).findByCommitSha("abc123def456");
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_EmptyCommits() {
        // 设置空提交列表
        pushEvent.setCommits(Collections.emptyList());
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(0, result);
        verify(commitRepository, never()).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_NullCommits() {
        // 设置null提交列表
        pushEvent.setCommits(null);
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(0, result);
        verify(commitRepository, never()).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_ExistingCommit() {
        // 模拟数据库中已存在该提交
        Commit existingCommit = new Commit();
        existingCommit.setCommitSha("abc123def456");
        when(commitRepository.findByCommitSha("abc123def456")).thenReturn(Optional.of(existingCommit));
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(0, result);
        verify(commitRepository, times(1)).findByCommitSha("abc123def456");
        verify(commitRepository, never()).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_MergeCommit() {
        // 设置合并提交消息
        commitInfo.setMessage("Merge branch 'feature/new-feature' into main");
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果 - 合并提交应该被跳过
        assertEquals(0, result);
        verify(commitRepository, times(1)).findByCommitSha("abc123def456");
        verify(commitRepository, never()).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_MultipleCommits() {
        // 创建第二个提交
        PushEventRequest.CommitInfo commitInfo2 = new PushEventRequest.CommitInfo();
        commitInfo2.setId("def456ghi789");
        commitInfo2.setMessage("Fix bug in existing feature");
        commitInfo2.setTimestamp("2023-12-01T11:00:00");
        commitInfo2.setAuthor(authorInfo);
        commitInfo2.setModified(Arrays.asList("src/main/java/BuggyClass.java"));
        
        pushEvent.setCommits(Arrays.asList(commitInfo, commitInfo2));
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(2, result);
        verify(commitRepository, times(2)).findByCommitSha(anyString());
        verify(commitRepository, times(2)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_DuplicateCommitsInSameEvent() {
        // 创建重复的提交
        PushEventRequest.CommitInfo duplicateCommit = new PushEventRequest.CommitInfo();
        duplicateCommit.setId("abc123def456"); // 相同的ID
        duplicateCommit.setMessage("Duplicate commit");
        duplicateCommit.setTimestamp("2023-12-01T11:00:00");
        duplicateCommit.setAuthor(authorInfo);
        
        pushEvent.setCommits(Arrays.asList(commitInfo, duplicateCommit));
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果 - 只应该处理一个提交
        assertEquals(1, result);
        verify(commitRepository, times(1)).findByCommitSha("abc123def456");
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_WithoutAuthorInfo() {
        // 移除author信息，应该使用push事件中的用户信息
        commitInfo.setAuthor(null);
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit savedCommit = invocation.getArgument(0);
            // 验证使用了push事件中的用户信息
            assertEquals("Test User", savedCommit.getDeveloperName());
            assertEquals("test@example.com", savedCommit.getDeveloperId());
            return savedCommit;
        });
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_CodeChangeStatsCalculation() {
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit savedCommit = invocation.getArgument(0);
            
            // 验证代码变更统计
            // 2个新增文件 * 50行 + 1个修改文件 * 5行 = 105行新增
            assertEquals(Integer.valueOf(105), savedCommit.getLinesAdded());
            // 1个删除文件 * 50行 + 1个修改文件 * 5行 = 55行删除
            assertEquals(Integer.valueOf(55), savedCommit.getLinesDeleted());
            // 总共4个文件变更
            assertEquals(Integer.valueOf(4), savedCommit.getFilesChanged());
            
            return savedCommit;
        });
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_BranchExtraction() {
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit savedCommit = invocation.getArgument(0);
            
            // 验证分支名称提取
            assertEquals("main", savedCommit.getBranch());
            
            return savedCommit;
        });
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_TagRef() {
        // 测试标签引用
        pushEvent.setRef("refs/tags/v1.0.0");
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit savedCommit = invocation.getArgument(0);
            
            // 验证标签名称提取
            assertEquals("v1.0.0", savedCommit.getBranch());
            
            return savedCommit;
        });
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
    
    @Test
    public void testProcessPushEvent_InvalidTimestamp() {
        // 设置无效的时间戳
        commitInfo.setTimestamp("invalid-timestamp");
        
        when(commitRepository.findByCommitSha(anyString())).thenReturn(Optional.empty());
        when(commitRepository.save(any(Commit.class))).thenAnswer(invocation -> {
            Commit savedCommit = invocation.getArgument(0);
            
            // 验证时间戳不为null（应该使用当前时间）
            assertNotNull(savedCommit.getTimestamp());
            
            return savedCommit;
        });
        
        // 执行测试
        int result = commitAnalysisService.processPushEvent(pushEvent);
        
        // 验证结果
        assertEquals(1, result);
        verify(commitRepository, times(1)).save(any(Commit.class));
    }
}