package com.gitlab.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * HTTPS安全配置类
 * 强制使用HTTPS传输，配置安全头
 */
@Configuration
@Profile("prod")
public class HttpsSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 强制使用HTTPS
            .requiresChannel(channel -> 
                channel.requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                       .requiresSecure())
            
            // 配置安全头
            .headers(headers -> headers
                // 启用HSTS (HTTP Strict Transport Security)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1年
                    .includeSubDomains(true))
                
                // 配置内容安全策略
                .contentSecurityPolicy("default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none';")
                
                // 防止点击劫持
                .and().frameOptions().deny()
                
                // 防止MIME类型嗅探
                .contentTypeOptions().and()
                
                // 配置Referrer策略
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                
                // 启用XSS保护
                .and().xssProtection().and()
            );
    }
}