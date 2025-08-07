package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.User;
import com.gitlab.metrics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户服务
 * 提供用户管理相关的业务逻辑
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 创建用户
     */
    public User createUser(String username, String password, String email, String fullName, Set<User.Role> roles) {
        logger.info("创建用户: username={}, email={}", username, email);
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        
        // 检查邮箱是否已存在
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRoles(roles);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        
        User savedUser = userRepository.save(user);
        logger.info("用户创建成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return savedUser;
    }
    
    /**
     * 根据用户名查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据邮箱查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 获取启用的用户
     */
    @Transactional(readOnly = true)
    public List<User> getEnabledUsers() {
        return userRepository.findByEnabledTrue();
    }
    
    /**
     * 根据角色查找用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * 更新用户最后登录时间和IP
     */
    public void updateLastLogin(Long userId, LocalDateTime loginTime, String loginIp) {
        userRepository.updateLastLogin(userId, loginTime, loginIp);
        logger.debug("更新用户最后登录信息: userId={}, loginTime={}, loginIp={}", 
                    userId, loginTime, loginIp);
    }
    
    /**
     * 启用用户
     */
    public void enableUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(true);
            userRepository.save(user);
            logger.info("用户已启用: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 禁用用户
     */
    public void disableUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(false);
            userRepository.save(user);
            logger.info("用户已禁用: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 锁定用户账户
     */
    public void lockUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountNonLocked(false);
            userRepository.save(user);
            logger.info("用户账户已锁定: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 解锁用户账户
     */
    public void unlockUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountNonLocked(true);
            userRepository.save(user);
            logger.info("用户账户已解锁: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 更新用户密码
     */
    public void updatePassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            logger.info("用户密码已更新: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 更新用户角色
     */
    public void updateUserRoles(Long userId, Set<User.Role> roles) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRoles(roles);
            userRepository.save(user);
            logger.info("用户角色已更新: userId={}, username={}, roles={}", 
                       userId, user.getUsername(), roles);
        }
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.delete(user);
            logger.info("用户已删除: userId={}, username={}", userId, user.getUsername());
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * 获取最近登录的用户
     */
    @Transactional(readOnly = true)
    public List<User> getRecentlyLoggedInUsers(LocalDateTime since) {
        return userRepository.findRecentlyLoggedInUsers(since);
    }
    
    /**
     * 获取长时间未登录的用户
     */
    @Transactional(readOnly = true)
    public List<User> getInactiveUsers(LocalDateTime threshold) {
        return userRepository.findInactiveUsers(threshold);
    }
    
    /**
     * 统计用户数量按角色
     */
    @Transactional(readOnly = true)
    public List<Object[]> countUsersByRole() {
        return userRepository.countUsersByRole();
    }
}