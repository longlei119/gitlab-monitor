package com.gitlab.metrics.security;

import com.gitlab.metrics.entity.User;
import com.gitlab.metrics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 * 实现Spring Security的UserDetailsService接口
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("加载用户详情: username={}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        return new CustomUserPrincipal(user);
    }
    
    /**
     * 自定义用户主体类
     */
    public static class CustomUserPrincipal implements UserDetails {
        
        private final User user;
        
        public CustomUserPrincipal(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getUsername();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return user.getAccountNonExpired();
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return user.getAccountNonLocked();
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return user.getCredentialsNonExpired();
        }
        
        @Override
        public boolean isEnabled() {
            return user.getEnabled();
        }
        
        /**
         * 获取用户实体
         */
        public User getUser() {
            return user;
        }
        
        /**
         * 获取用户ID
         */
        public Long getUserId() {
            return user.getId();
        }
        
        /**
         * 获取用户全名
         */
        public String getFullName() {
            return user.getFullName();
        }
        
        /**
         * 获取用户邮箱
         */
        public String getEmail() {
            return user.getEmail();
        }
        
        /**
         * 检查用户是否具有指定角色
         */
        public boolean hasRole(User.Role role) {
            return user.getRoles().contains(role);
        }
        
        /**
         * 检查用户是否为管理员
         */
        public boolean isAdmin() {
            return hasRole(User.Role.ADMIN);
        }
        
        /**
         * 检查用户是否为开发者
         */
        public boolean isDeveloper() {
            return hasRole(User.Role.DEVELOPER);
        }
        
        /**
         * 检查用户是否为项目经理
         */
        public boolean isProjectManager() {
            return hasRole(User.Role.PROJECT_MANAGER);
        }
    }
}