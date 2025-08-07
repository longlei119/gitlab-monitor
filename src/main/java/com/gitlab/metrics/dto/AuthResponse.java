package com.gitlab.metrics.dto;

import com.gitlab.metrics.entity.User;

import java.util.Set;

/**
 * 认证响应DTO
 * 用于返回登录成功后的用户信息和令牌
 */
public class AuthResponse {
    
    private String token;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private String fullName;
    private String email;
    private Set<User.Role> roles;
    private Boolean success = true;
    private String message;
    private String error;
    
    public AuthResponse() {}
    
    public AuthResponse(String token, String tokenType, Long expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }
    
    /**
     * 创建错误响应
     */
    public static AuthResponse error(String errorMessage) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        response.setMessage(errorMessage);
        return response;
    }
    
    /**
     * 创建成功响应
     */
    public static AuthResponse success(String token, String username) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUsername(username);
        response.setSuccess(true);
        response.setMessage("登录成功");
        return response;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Set<User.Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<User.Role> roles) {
        this.roles = roles;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}