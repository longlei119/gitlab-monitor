package com.gitlab.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GitLab研发度量系统后端应用 - 简化版本
 * 用于测试性能优化功能
 */
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableCaching
@EnableAsync
public class GitlabMetricsSimpleApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "simple");
        SpringApplication.run(GitlabMetricsSimpleApplication.class, args);
    }
}