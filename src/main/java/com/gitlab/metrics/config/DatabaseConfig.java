package com.gitlab.metrics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.gitlab.metrics.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA configuration is handled by application.yml
    // This class enables JPA repositories and auditing
}