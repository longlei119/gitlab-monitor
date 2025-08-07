package com.gitlab.metrics.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for GitLab push events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushEventRequest extends WebhookRequest {
    
    @JsonProperty("before")
    private String before;
    
    @JsonProperty("after")
    private String after;
    
    @JsonProperty("ref")
    private String ref;
    
    @JsonProperty("checkout_sha")
    private String checkoutSha;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("user_name")
    private String userName;
    
    @JsonProperty("user_username")
    private String userUsername;
    
    @JsonProperty("user_email")
    private String userEmail;
    
    @JsonProperty("project_id")
    private Long projectId;
    
    @JsonProperty("commits")
    private List<CommitInfo> commits;
    
    @JsonProperty("total_commits_count")
    private Integer totalCommitsCount;
    
    // Getters and Setters
    public String getBefore() {
        return before;
    }
    
    public void setBefore(String before) {
        this.before = before;
    }
    
    public String getAfter() {
        return after;
    }
    
    public void setAfter(String after) {
        this.after = after;
    }
    
    public String getRef() {
        return ref;
    }
    
    public void setRef(String ref) {
        this.ref = ref;
    }
    
    public String getCheckoutSha() {
        return checkoutSha;
    }
    
    public void setCheckoutSha(String checkoutSha) {
        this.checkoutSha = checkoutSha;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserUsername() {
        return userUsername;
    }
    
    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public List<CommitInfo> getCommits() {
        return commits;
    }
    
    public void setCommits(List<CommitInfo> commits) {
        this.commits = commits;
    }
    
    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
    }
    
    public void setTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitInfo {
        private String id;
        private String message;
        private String timestamp;
        private String url;
        private AuthorInfo author;
        private List<String> added;
        private List<String> modified;
        private List<String> removed;
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public AuthorInfo getAuthor() {
            return author;
        }
        
        public void setAuthor(AuthorInfo author) {
            this.author = author;
        }
        
        public List<String> getAdded() {
            return added;
        }
        
        public void setAdded(List<String> added) {
            this.added = added;
        }
        
        public List<String> getModified() {
            return modified;
        }
        
        public void setModified(List<String> modified) {
            this.modified = modified;
        }
        
        public List<String> getRemoved() {
            return removed;
        }
        
        public void setRemoved(List<String> removed) {
            this.removed = removed;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorInfo {
        private String name;
        private String email;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
}