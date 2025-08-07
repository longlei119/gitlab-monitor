package com.gitlab.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SonarQube配置属性类
 * 用于管理SonarQube相关的配置参数
 */
@Component
@ConfigurationProperties(prefix = "sonarqube")
public class SonarQubeProperties {
    
    private String url = "http://localhost:9000";
    private String token;
    private int timeout = 30000;
    private Analysis analysis = new Analysis();
    private Projects projects = new Projects();
    
    public static class Analysis {
        private boolean enabled = true;
        private boolean triggerOnCommit = true;
        private long qualityGateTimeout = 300000L; // 5 minutes
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isTriggerOnCommit() {
            return triggerOnCommit;
        }
        
        public void setTriggerOnCommit(boolean triggerOnCommit) {
            this.triggerOnCommit = triggerOnCommit;
        }
        
        public long getQualityGateTimeout() {
            return qualityGateTimeout;
        }
        
        public void setQualityGateTimeout(long qualityGateTimeout) {
            this.qualityGateTimeout = qualityGateTimeout;
        }
    }
    
    public static class Projects {
        private String defaultBranch = "main";
        
        public String getDefaultBranch() {
            return defaultBranch;
        }
        
        public void setDefaultBranch(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public Analysis getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }
    
    public Projects getProjects() {
        return projects;
    }
    
    public void setProjects(Projects projects) {
        this.projects = projects;
    }
}