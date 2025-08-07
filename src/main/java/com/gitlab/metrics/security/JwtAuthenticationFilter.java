package com.gitlab.metrics.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 * 处理JWT令牌的验证和用户认证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String requestHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();
        
        String username = null;
        String authToken = null;
        
        // 从请求头中提取JWT令牌
        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {
            authToken = jwtTokenUtil.getTokenFromHeader(requestHeader);
            try {
                username = jwtTokenUtil.getUsernameFromToken(authToken);
            } catch (Exception e) {
                logger.warn("无法从JWT令牌中获取用户名: {}", e.getMessage());
            }
        } else {
            logger.debug("请求未包含JWT令牌: {}", requestURI);
        }
        
        // 验证令牌并设置认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("用户认证成功: username={}, authorities={}", 
                               username, userDetails.getAuthorities());
                } else {
                    logger.warn("JWT令牌验证失败: username={}", username);
                }
            } catch (Exception e) {
                logger.error("用户认证过程中发生错误: {}", e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 跳过不需要认证的路径
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/api/v1/webhook/") ||
               path.equals("/api/v1/health") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }
}