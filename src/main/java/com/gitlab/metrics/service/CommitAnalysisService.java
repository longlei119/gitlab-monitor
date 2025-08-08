package com.gitlab.metrics.service;

import com.gitlab.metrics.dto.webhook.PushEventRequest;
import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.entity.FileChange;
import com.gitlab.metrics.repository.CommitRepository;
import com.gitlab.metrics.repository.FileChangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 代码提交分析服务
 * 处理GitLab push事件，分析代码提交数据，包括代码行数统计和合并提交处理
 */
@Service
@Transactional
public class CommitAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitAnalysisService.class);
    
    // 合并提交的正则表达式模式
    private static final Pattern MERGE_COMMIT_PATTERN = Pattern.compile(
        "^Merge\\s+(branch|pull\\s+request|remote-tracking\\s+branch)", 
        Pattern.CASE_INSENSITIVE
    );
    
    @Autowired
    private CommitRepository commitRepository;
    
    @Autowired
    private FileChangeRepository fileChangeRepository;
    
    @Autowired
    private SonarQubeAnalysisService sonarQubeAnalysisService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private BatchProcessingService batchProcessingService;
    
    /**
     * 处理push事件，分析并保存提交数据（优化版本）
     * 使用批量处理和异步操作提高性能
     * 
     * @param pushEvent GitLab push事件数据
     * @return 处理的提交数量
     */
    public int processPushEvent(PushEventRequest pushEvent) {
        logger.info("Processing push event for project: {}, ref: {}, commits: {}", 
                   pushEvent.getProjectId(), pushEvent.getRef(), 
                   pushEvent.getCommits() != null ? pushEvent.getCommits().size() : 0);
        
        if (pushEvent.getCommits() == null || pushEvent.getCommits().isEmpty()) {
            logger.debug("No commits found in push event");
            return 0;
        }
        
        String branch = extractBranchFromRef(pushEvent.getRef());
        String projectId = String.valueOf(pushEvent.getProjectId());
        
        // 批量处理提交数据
        List<Commit> commitsToSave = new ArrayList<>();
        List<FileChange> fileChangesToSave = new ArrayList<>();
        Set<String> processedCommitShas = new HashSet<>();
        
        // 批量检查已存在的提交
        Set<String> existingCommitShas = batchCheckExistingCommits(
            pushEvent.getCommits().stream()
                .map(PushEventRequest.CommitInfo::getId)
                .collect(java.util.stream.Collectors.toList())
        );
        
        for (PushEventRequest.CommitInfo commitInfo : pushEvent.getCommits()) {
            try {
                // 避免重复处理相同的提交
                if (processedCommitShas.contains(commitInfo.getId()) || 
                    existingCommitShas.contains(commitInfo.getId())) {
                    logger.debug("Skipping duplicate or existing commit: {}", commitInfo.getId());
                    continue;
                }
                
                Commit commit = processCommitInfo(commitInfo, projectId, branch, pushEvent);
                if (commit != null) {
                    commitsToSave.add(commit);
                    fileChangesToSave.addAll(commit.getFileChanges());
                    processedCommitShas.add(commitInfo.getId());
                    
                    // 异步触发代码质量分析
                    triggerQualityAnalysisAsync(projectId, commitInfo.getId(), pushEvent);
                }
                
            } catch (Exception e) {
                logger.error("Failed to process commit: {}", commitInfo.getId(), e);
                // 继续处理其他提交，不因单个提交失败而中断整个处理流程
            }
        }
        
        // 批量保存提交数据
        if (!commitsToSave.isEmpty()) {
            try {
                batchProcessingService.batchSaveCommits(commitsToSave);
                logger.info("Successfully queued {} commits for batch processing", commitsToSave.size());
            } catch (Exception e) {
                logger.error("Failed to batch save commits", e);
                // 降级到单个保存
                fallbackToIndividualSave(commitsToSave);
            }
        }
        
        logger.info("Successfully processed {} commits from push event", commitsToSave.size());
        return commitsToSave.size();
    }
    
    /**
     * 批量检查已存在的提交
     * 
     * @param commitShas 提交SHA列表
     * @return 已存在的提交SHA集合
     */
    private Set<String> batchCheckExistingCommits(List<String> commitShas) {
        if (commitShas.isEmpty()) {
            return new HashSet<>();
        }
        
        try {
            // 使用IN查询批量检查
            String jpql = "SELECT c.commitSha FROM Commit c WHERE c.commitSha IN :commitShas";
            List<String> existingCommits = entityManager.createQuery(jpql, String.class)
                .setParameter("commitShas", commitShas)
                .getResultList();
            
            return new HashSet<>(existingCommits);
        } catch (Exception e) {
            logger.warn("Failed to batch check existing commits, falling back to individual checks", e);
            return new HashSet<>();
        }
    }
    
    /**
     * 降级到单个保存（当批量保存失败时）
     * 
     * @param commits 提交列表
     */
    private void fallbackToIndividualSave(List<Commit> commits) {
        logger.info("Falling back to individual save for {} commits", commits.size());
        
        int savedCount = 0;
        for (Commit commit : commits) {
            try {
                commitRepository.save(commit);
                savedCount++;
            } catch (Exception e) {
                logger.error("Failed to save individual commit: {}", commit.getCommitSha(), e);
            }
        }
        
        logger.info("Individual save completed, saved {} out of {} commits", savedCount, commits.size());
    }
    
    /**
     * 异步触发代码质量分析
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param pushEvent push事件数据
     */
    @Async("taskExecutor")
    private void triggerQualityAnalysisAsync(String projectId, String commitSha, PushEventRequest pushEvent) {
        try {
            // 构建SonarQube项目键，通常使用项目路径或名称
            String sonarProjectKey = generateSonarProjectKey(pushEvent);
            
            logger.debug("Triggering async quality analysis for project: {}, commit: {}, sonar key: {}", 
                        projectId, commitSha, sonarProjectKey);
            
            // 异步触发质量分析
            sonarQubeAnalysisService.triggerQualityAnalysis(projectId, commitSha, sonarProjectKey);
            
        } catch (Exception e) {
            logger.error("Failed to trigger async quality analysis for commit: {}", commitSha, e);
            // 不抛出异常，避免影响主要的提交处理流程
        }
    }
    
    /**
     * 处理单个提交信息
     * 
     * @param commitInfo 提交信息
     * @param projectId 项目ID
     * @param branch 分支名称
     * @param pushEvent 原始push事件（用于获取用户信息）
     * @return 处理后的Commit实体，如果是合并提交则返回null
     */
    private Commit processCommitInfo(PushEventRequest.CommitInfo commitInfo, String projectId, 
                                   String branch, PushEventRequest pushEvent) {
        
        // 检查是否为合并提交，如果是则跳过以避免重复统计
        if (isMergeCommit(commitInfo.getMessage())) {
            logger.debug("Skipping merge commit: {} - {}", commitInfo.getId(), commitInfo.getMessage());
            return null;
        }
        
        Commit commit = new Commit();
        commit.setCommitSha(commitInfo.getId());
        commit.setProjectId(projectId);
        commit.setBranch(branch);
        commit.setMessage(commitInfo.getMessage());
        
        // 设置开发者信息，优先使用commit中的author信息
        if (commitInfo.getAuthor() != null) {
            commit.setDeveloperName(commitInfo.getAuthor().getName());
            commit.setDeveloperId(commitInfo.getAuthor().getEmail());
        } else {
            // 如果commit中没有author信息，使用push事件中的用户信息
            commit.setDeveloperName(pushEvent.getUserName());
            commit.setDeveloperId(pushEvent.getUserEmail());
        }
        
        // 解析时间戳
        commit.setTimestamp(parseTimestamp(commitInfo.getTimestamp()));
        
        // 计算代码行数变更统计
        CodeChangeStats stats = calculateCodeChangeStats(commitInfo);
        commit.setLinesAdded(stats.getLinesAdded());
        commit.setLinesDeleted(stats.getLinesDeleted());
        commit.setFilesChanged(stats.getFilesChanged());
        
        // 创建文件变更记录
        List<FileChange> fileChanges = createFileChanges(commit, commitInfo);
        commit.setFileChanges(fileChanges);
        
        return commit;
    }
    
    /**
     * 判断是否为合并提交
     * 
     * @param message 提交消息
     * @return true如果是合并提交
     */
    private boolean isMergeCommit(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        
        return MERGE_COMMIT_PATTERN.matcher(message.trim()).find();
    }
    
    /**
     * 从ref中提取分支名称
     * 
     * @param ref Git引用，如 refs/heads/main
     * @return 分支名称，如 main
     */
    private String extractBranchFromRef(String ref) {
        if (!StringUtils.hasText(ref)) {
            return "unknown";
        }
        
        if (ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        } else if (ref.startsWith("refs/tags/")) {
            return ref.substring("refs/tags/".length());
        }
        
        return ref;
    }
    
    /**
     * 解析时间戳字符串
     * 
     * @param timestamp 时间戳字符串
     * @return LocalDateTime对象
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return LocalDateTime.now();
        }
        
        try {
            // GitLab通常使用ISO 8601格式：2023-01-01T12:00:00Z
            if (timestamp.endsWith("Z")) {
                timestamp = timestamp.substring(0, timestamp.length() - 1);
            }
            
            // 尝试不同的时间格式
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(timestamp, formatter);
                } catch (Exception ignored) {
                    // 尝试下一个格式
                }
            }
            
            logger.warn("Unable to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
            
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}, using current time", timestamp, e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 计算代码变更统计
     * 
     * @param commitInfo 提交信息
     * @return 代码变更统计结果
     */
    private CodeChangeStats calculateCodeChangeStats(PushEventRequest.CommitInfo commitInfo) {
        CodeChangeStats stats = new CodeChangeStats();
        
        // 统计文件数量变更
        int filesChanged = 0;
        if (commitInfo.getAdded() != null) {
            filesChanged += commitInfo.getAdded().size();
        }
        if (commitInfo.getModified() != null) {
            filesChanged += commitInfo.getModified().size();
        }
        if (commitInfo.getRemoved() != null) {
            filesChanged += commitInfo.getRemoved().size();
        }
        
        stats.setFilesChanged(filesChanged);
        
        // 注意：GitLab webhook中的added/modified/removed字段只包含文件路径，
        // 不包含具体的行数变更信息。实际的行数统计需要通过Git API获取。
        // 这里我们使用一个简化的估算方法，或者标记为需要后续处理。
        
        // 简化估算：新增文件按平均50行计算，删除文件按平均50行计算
        // 修改文件按平均10行变更计算（5行新增+5行删除）
        int estimatedLinesAdded = 0;
        int estimatedLinesDeleted = 0;
        
        if (commitInfo.getAdded() != null) {
            estimatedLinesAdded += commitInfo.getAdded().size() * 50;
        }
        
        if (commitInfo.getRemoved() != null) {
            estimatedLinesDeleted += commitInfo.getRemoved().size() * 50;
        }
        
        if (commitInfo.getModified() != null) {
            estimatedLinesAdded += commitInfo.getModified().size() * 5;
            estimatedLinesDeleted += commitInfo.getModified().size() * 5;
        }
        
        stats.setLinesAdded(estimatedLinesAdded);
        stats.setLinesDeleted(estimatedLinesDeleted);
        
        logger.debug("Calculated code change stats for commit {}: files={}, lines added={}, lines deleted={}", 
                    commitInfo.getId(), filesChanged, estimatedLinesAdded, estimatedLinesDeleted);
        
        return stats;
    }
    
    /**
     * 创建文件变更记录
     * 
     * @param commit 提交实体
     * @param commitInfo 提交信息
     * @return 文件变更记录列表
     */
    private List<FileChange> createFileChanges(Commit commit, PushEventRequest.CommitInfo commitInfo) {
        List<FileChange> fileChanges = new ArrayList<>();
        
        // 处理新增文件
        if (commitInfo.getAdded() != null) {
            for (String filePath : commitInfo.getAdded()) {
                FileChange fileChange = new FileChange();
                fileChange.setCommit(commit);
                fileChange.setFilePath(filePath);
                fileChange.setChangeType("added");
                fileChange.setLinesAdded(50); // 估算值
                fileChange.setLinesDeleted(0);
                fileChanges.add(fileChange);
            }
        }
        
        // 处理修改文件
        if (commitInfo.getModified() != null) {
            for (String filePath : commitInfo.getModified()) {
                FileChange fileChange = new FileChange();
                fileChange.setCommit(commit);
                fileChange.setFilePath(filePath);
                fileChange.setChangeType("modified");
                fileChange.setLinesAdded(5); // 估算值
                fileChange.setLinesDeleted(5); // 估算值
                fileChanges.add(fileChange);
            }
        }
        
        // 处理删除文件
        if (commitInfo.getRemoved() != null) {
            for (String filePath : commitInfo.getRemoved()) {
                FileChange fileChange = new FileChange();
                fileChange.setCommit(commit);
                fileChange.setFilePath(filePath);
                fileChange.setChangeType("removed");
                fileChange.setLinesAdded(0);
                fileChange.setLinesDeleted(50); // 估算值
                fileChanges.add(fileChange);
            }
        }
        
        return fileChanges;
    }
    
    /**
     * 触发代码质量分析
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param pushEvent push事件数据
     */
    private void triggerQualityAnalysis(String projectId, String commitSha, PushEventRequest pushEvent) {
        try {
            // 构建SonarQube项目键，通常使用项目路径或名称
            String sonarProjectKey = generateSonarProjectKey(pushEvent);
            
            logger.debug("Triggering quality analysis for project: {}, commit: {}, sonar key: {}", 
                        projectId, commitSha, sonarProjectKey);
            
            // 异步触发质量分析
            sonarQubeAnalysisService.triggerQualityAnalysis(projectId, commitSha, sonarProjectKey);
            
        } catch (Exception e) {
            logger.error("Failed to trigger quality analysis for commit: {}", commitSha, e);
            // 不抛出异常，避免影响主要的提交处理流程
        }
    }
    
    /**
     * 生成SonarQube项目键
     * 
     * @param pushEvent push事件数据
     * @return SonarQube项目键
     */
    private String generateSonarProjectKey(PushEventRequest pushEvent) {
        // 优先使用项目路径，如果没有则使用项目名称，最后使用项目ID
        if (pushEvent.getProject() != null) {
            if (pushEvent.getProject().getPathWithNamespace() != null) {
                return pushEvent.getProject().getPathWithNamespace().replace("/", ":");
            } else if (pushEvent.getProject().getName() != null) {
                return pushEvent.getProject().getName().toLowerCase().replace(" ", "-");
            }
        }
        
        // 如果都没有，使用项目ID
        return "project-" + pushEvent.getProjectId();
    }
    
    /**
     * 代码变更统计内部类
     */
    private static class CodeChangeStats {
        private int linesAdded;
        private int linesDeleted;
        private int filesChanged;
        
        public int getLinesAdded() {
            return linesAdded;
        }
        
        public void setLinesAdded(int linesAdded) {
            this.linesAdded = linesAdded;
        }
        
        public int getLinesDeleted() {
            return linesDeleted;
        }
        
        public void setLinesDeleted(int linesDeleted) {
            this.linesDeleted = linesDeleted;
        }
        
        public int getFilesChanged() {
            return filesChanged;
        }
        
        public void setFilesChanged(int filesChanged) {
            this.filesChanged = filesChanged;
        }
    }
}