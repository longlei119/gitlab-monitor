package com.gitlab.metrics.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据库SSL配置类
 * 配置MySQL数据库的SSL连接
 */
@Configuration
@Profile("prod")
public class DatabaseSslConfig {
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.hikari.data-source-properties.trustCertificateKeyStoreUrl:}")
    private String trustStoreUrl;
    
    @Value("${spring.datasource.hikari.data-source-properties.trustCertificateKeyStorePassword:}")
    private String trustStorePassword;
    
    @Value("${spring.datasource.hikari.data-source-properties.clientCertificateKeyStoreUrl:}")
    private String clientCertUrl;
    
    @Value("${spring.datasource.hikari.data-source-properties.clientCertificateKeyStorePassword:}")
    private String clientCertPassword;
    
    /**
     * 配置支持SSL的数据源
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setIdleTimeout(600000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        
        // SSL配置
        Properties props = new Properties();
        props.setProperty("useSSL", "true");
        props.setProperty("requireSSL", "true");
        props.setProperty("verifyServerCertificate", "true");
        props.setProperty("useUnicode", "true");
        props.setProperty("characterEncoding", "UTF-8");
        props.setProperty("serverTimezone", "UTC");
        
        // 配置信任存储
        if (trustStoreUrl != null && !trustStoreUrl.isEmpty()) {
            props.setProperty("trustCertificateKeyStoreUrl", trustStoreUrl);
            props.setProperty("trustCertificateKeyStorePassword", trustStorePassword);
        }
        
        // 配置客户端证书
        if (clientCertUrl != null && !clientCertUrl.isEmpty()) {
            props.setProperty("clientCertificateKeyStoreUrl", clientCertUrl);
            props.setProperty("clientCertificateKeyStorePassword", clientCertPassword);
        }
        
        // 启用SSL相关的MySQL连接器配置
        props.setProperty("sslMode", "REQUIRED");
        props.setProperty("allowPublicKeyRetrieval", "false");
        
        config.setDataSourceProperties(props);
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        return new HikariDataSource(config);
    }
}