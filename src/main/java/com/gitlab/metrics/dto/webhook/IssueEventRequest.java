package com.gitlab.metrics.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitLab issue events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueEventRequest extends WebhookRequest {
    
    @JsonProperty("object_attributes")
    private IssueAttributes objectAttributes;
    
    @JsonProperty("assignees")
    private UserInfo[] assignees;
    
    @JsonProperty("assignee")
    private UserInfo assignee;
    
    @JsonProperty("changes")
    private IssueChanges changes;
    
    // Getters and Setters
    public IssueAttributes getObjectAttributes() {
        return objectAttributes;
    }
    
    public void setObjectAttributes(IssueAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }
    
    public UserInfo[] getAssignees() {
        return assignees;
    }
    
    public void setAssignees(UserInfo[] assignees) {
        this.assignees = assignees;
    }
    
    public UserInfo getAssignee() {
        return assignee;
    }
    
    public void setAssignee(UserInfo assignee) {
        this.assignee = assignee;
    }
    
    public IssueChanges getChanges() {
        return changes;
    }
    
    public void setChanges(IssueChanges changes) {
        this.changes = changes;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueAttributes {
        private Long id;
        private Long iid;
        private String title;
        private String description;
        private String state;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("closed_at")
        private String closedAt;
        @JsonProperty("author_id")
        private Long authorId;
        @JsonProperty("assignee_id")
        private Long assigneeId;
        @JsonProperty("project_id")
        private Long projectId;
        private String action;
        private String url;
        private String[] labels;
        
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
        
        public String getClosedAt() {
            return closedAt;
        }
        
        public void setClosedAt(String closedAt) {
            this.closedAt = closedAt;
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
        
        public Long getProjectId() {
            return projectId;
        }
        
        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String[] getLabels() {
            return labels;
        }
        
        public void setLabels(String[] labels) {
            this.labels = labels;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueChanges {
        @JsonProperty("updated_at")
        private ChangeInfo updatedAt;
        
        @JsonProperty("state_id")
        private ChangeInfo stateId;
        
        // Getters and Setters
        public ChangeInfo getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(ChangeInfo updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public ChangeInfo getStateId() {
            return stateId;
        }
        
        public void setStateId(ChangeInfo stateId) {
            this.stateId = stateId;
        }
        
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ChangeInfo {
            private Object previous;
            private Object current;
            
            // Getters and Setters
            public Object getPrevious() {
                return previous;
            }
            
            public void setPrevious(Object previous) {
                this.previous = previous;
            }
            
            public Object getCurrent() {
                return current;
            }
            
            public void setCurrent(Object current) {
                this.current = current;
            }
        }
    }
}