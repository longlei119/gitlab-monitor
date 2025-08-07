package com.gitlab.metrics.controller;

import com.gitlab.metrics.dto.AuthRequest;
import com.gitlab.metrics.dto.AuthResponse;
import com.gitlab.metrics.entity.User;
import com.gitlab.metrics.repository.UserRepository;
import com.gitlab.metrics.security.CustomUserDetailsService;
import com.gitlab.metrics.security.JwtTokenUtil;
import com.gitlab.metrics.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 认证控制器
 * 处理用户登录、注册、令牌刷新等认证相关操作
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest, 
                                            HttpServletRequest request) {
        logger.info("用户登录请求: username={}", authRequest.getUsername());
        
        try {
            // 认证用户
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(), 
                    authRequest.getPassword()
                )
            );
            
            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (CustomUserDetailsService.CustomUserPrincipal) userDetails;
            
            // 生成JWT令牌
            String token = jwtTokenUtil.generateToken(userDetails);
            
            // 更新用户最后登录时间和IP
            String clientIp = getClientIp(request);
            userService.updateLastLogin(userPrincipal.getUserId(), LocalDateTime.now(), clientIp);
            
            // 构建响应
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setTokenType("Bearer");
            response.setExpiresIn(86400L); // 24小时
            response.setUsername(userPrincipal.getUsername());
            response.setFullName(userPrincipal.getFullName());
            response.setEmail(userPrincipal.getEmail());
            response.setRoles(userPrincipal.getUser().getRoles());
            
            logger.info("用户登录成功: username={}, ip={}", authRequest.getUsername(), clientIp);
            return ResponseEntity.ok(response);
            
        } catch (DisabledException e) {
            logger.warn("用户账户已禁用: username={}", authRequest.getUsername());
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("账户已禁用"));
        } catch (BadCredentialsException e) {
            logger.warn("用户登录失败，凭据错误: username={}", authRequest.getUsername());
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("用户名或密码错误"));
        } catch (Exception e) {
            logger.error("用户登录过程中发生错误: username={}, error={}", 
                        authRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(AuthResponse.error("登录失败，请稍后重试"));
        }
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        logger.info("刷新令牌请求");
        
        try {
            String token = jwtTokenUtil.getTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest()
                    .body(AuthResponse.error("无效的令牌格式"));
            }
            
            String username = jwtTokenUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtTokenUtil.validateToken(token, userDetails)) {
                String newToken = jwtTokenUtil.refreshToken(token);
                if (newToken != null) {
                    CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                        (CustomUserDetailsService.CustomUserPrincipal) userDetails;
                    
                    AuthResponse response = new AuthResponse();
                    response.setToken(newToken);
                    response.setTokenType("Bearer");
                    response.setExpiresIn(86400L);
                    response.setUsername(userPrincipal.getUsername());
                    response.setFullName(userPrincipal.getFullName());
                    response.setEmail(userPrincipal.getEmail());
                    response.setRoles(userPrincipal.getUser().getRoles());
                    
                    logger.info("令牌刷新成功: username={}", username);
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("令牌刷新失败"));
                
        } catch (Exception e) {
            logger.error("刷新令牌过程中发生错误: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("令牌刷新失败"));
        }
    }
    
    /**
     * 验证令牌
     */
    @PostMapping("/validate")
    public ResponseEntity<Object> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = jwtTokenUtil.getTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body("无效的令牌格式");
            }
            
            String username = jwtTokenUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            boolean isValid = jwtTokenUtil.validateToken(token, userDetails);
            
            if (isValid) {
                CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                    (CustomUserDetailsService.CustomUserPrincipal) userDetails;
                
                return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                    put("valid", true);
                    put("username", userPrincipal.getUsername());
                    put("fullName", userPrincipal.getFullName());
                    put("roles", userPrincipal.getUser().getRoles());
                    put("expiresAt", jwtTokenUtil.getExpirationDateFromToken(token));
                }});
            } else {
                return ResponseEntity.badRequest().body("令牌无效或已过期");
            }
            
        } catch (Exception e) {
            logger.error("验证令牌过程中发生错误: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("令牌验证失败");
        }
    }
    
    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("用户注销请求");
        
        try {
            String token = jwtTokenUtil.getTokenFromHeader(authHeader);
            if (token != null) {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                logger.info("用户注销成功: username={}", username);
                
                // 这里可以将令牌加入黑名单，实现真正的注销
                // 目前只是记录日志，客户端需要删除本地存储的令牌
            }
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", "注销成功");
            }});
            
        } catch (Exception e) {
            logger.error("用户注销过程中发生错误: {}", e.getMessage(), e);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", "注销成功");
            }});
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = jwtTokenUtil.getTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body("无效的令牌格式");
            }
            
            String username = jwtTokenUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (CustomUserDetailsService.CustomUserPrincipal) userDetails;
            
            User user = userPrincipal.getUser();
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("id", user.getId());
                put("username", user.getUsername());
                put("fullName", user.getFullName());
                put("email", user.getEmail());
                put("roles", user.getRoles());
                put("createdAt", user.getCreatedAt());
                put("lastLoginAt", user.getLastLoginAt());
            }});
            
        } catch (Exception e) {
            logger.error("获取当前用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("获取用户信息失败");
        }
    }
    
    // 辅助方法
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}