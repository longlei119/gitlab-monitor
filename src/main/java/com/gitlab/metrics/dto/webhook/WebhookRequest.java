package com.gitlab.metrics.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base webhook request DTO for GitLab webhook events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookRequest {
    
    @JsonProperty("object_kind")
    private String objectKind;
    
    @JsonProperty("event_name")
    private String eventName;
    
    @JsonProperty("project")
    private ProjectInfo project;
    
    @JsonProperty("user")
    private UserInfo user;
    
    // Constructors
    public WebhookRequest() {}
    
    // Getters and Setters
    public String getObjectKind() {
        return objectKind;
    }
    
    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    public ProjectInfo getProject() {
        return project;
    }
    
    public void setProject(ProjectInfo project) {
        this.project = project;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectInfo {
        private Long id;
        private String name;
        private String path;
        @JsonProperty("path_with_namespace")
        private String pathWithNamespace;
        @JsonProperty("web_url")
        private String webUrl;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getPathWithNamespace() {
            return pathWithNamespace;
        }
        
        public void setPathWithNamespace(String pathWithNamespace) {
            this.pathWithNamespace = pathWithNamespace;
        }
        
        public String getWebUrl() {
            return webUrl;
        }
        
        public void setWebUrl(String webUrl) {
            this.webUrl = webUrl;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private Long id;
        private String name;
        private String username;
        private String email;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
}