package com.gitlab.metrics.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitLab merge request events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequestEventRequest extends WebhookRequest {
    
    @JsonProperty("object_attributes")
    private MergeRequestAttributes objectAttributes;
    
    @JsonProperty("changes")
    private MergeRequestChanges changes;
    
    // Getters and Setters
    public MergeRequestAttributes getObjectAttributes() {
        return objectAttributes;
    }
    
    public void setObjectAttributes(MergeRequestAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }
    
    public MergeRequestChanges getChanges() {
        return changes;
    }
    
    public void setChanges(MergeRequestChanges changes) {
        this.changes = changes;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MergeRequestAttributes {
        private Long id;
        private Long iid;
        @JsonProperty("target_branch")
        private String targetBranch;
        @JsonProperty("source_branch")
        private String sourceBranch;
        @JsonProperty("source_project_id")
        private Long sourceProjectId;
        @JsonProperty("author_id")
        private Long authorId;
        @JsonProperty("assignee_id")
        private Long assigneeId;
        private String title;
        private String description;
        private String state;
        @JsonProperty("merge_status")
        private String mergeStatus;
        @JsonProperty("target_project_id")
        private Long targetProjectId;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("merge_commit_sha")
        private String mergeCommitSha;
        private String action;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getIid() {
            return iid;
        }
        
        public void setIid(Long iid) {
            this.iid = iid;
        }
        
        public String getTargetBranch() {
            return targetBranch;
        }
        
        public void setTargetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
        }
        
        public String getSourceBranch() {
            return sourceBranch;
        }
        
        public void setSourceBranch(String sourceBranch) {
            this.sourceBranch = sourceBranch;
        }
        
        public Long getSourceProjectId() {
            return sourceProjectId;
        }
        
        public void setSourceProjectId(Long sourceProjectId) {
            this.sourceProjectId = sourceProjectId;
        }
        
        public Long getAuthorId() {
            return authorId;
        }
        
        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }
        
        public Long getAssigneeId() {
            return assigneeId;
        }
        
        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getMergeStatus() {
            return mergeStatus;
        }
        
        public void setMergeStatus(String mergeStatus) {
            this.mergeStatus = mergeStatus;
        }
        
        public Long getTargetProjectId() {
            return targetProjectId;
        }
        
        public void setTargetProjectId(Long targetProjectId) {
            this.targetProjectId = targetProjectId;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public String getMergeCommitSha() {
            return mergeCommitSha;
        }
        
        public void setMergeCommitSha(String mergeCommitSha) {
            this.mergeCommitSha = mergeCommitSha;
        }
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MergeRequestChanges {
        @JsonProperty("updated_at")
        private ChangeInfo updatedAt;
        
        // Getters and Setters
        public ChangeInfo getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(ChangeInfo updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ChangeInfo {
            private String previous;
            private String current;
            
            // Getters and Setters
            public String getPrevious() {
                return previous;
            }
            
            public void setPrevious(String previous) {
                this.previous = previous;
            }
            
            public String getCurrent() {
                return current;
            }
            
            public void setCurrent(String current) {
                this.current = current;
            }
        }
    }
}