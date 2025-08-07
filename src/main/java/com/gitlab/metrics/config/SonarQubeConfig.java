package com.gitlab.metrics.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SonarQube配置类
 * 配置SonarQube HTTP客户端
 */
@Configuration
public class SonarQubeConfig {
    
    @Autowired
    private SonarQubeProperties sonarQubeProperties;
    
    /**
     * 创建SonarQube HTTP客户端
     */
    @Bean
    public CloseableHttpClient sonarQubeHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(sonarQubeProperties.getTimeout())
                .setSocketTimeout(sonarQubeProperties.getTimeout())
                .setConnectionRequestTimeout(sonarQubeProperties.getTimeout())
                .build();
        
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}