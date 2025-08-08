package com.gitlab.metrics.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Custom health indicator for database connectivity and performance
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("database", "Connection failed")
                .build();
        }
    }
    
    private Health checkDatabaseHealth() throws Exception {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            if (!connection.isValid(5)) {
                return Health.down()
                    .withDetail("database", "Connection validation failed")
                    .build();
            }
            
            // Test query performance
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        Health.Builder builder = Health.up()
                            .withDetail("database", "UP")
                            .withDetail("responseTime", responseTime + "ms")
                            .withDetail("validationQuery", "SELECT 1");
                        
                        // Add performance warning if response time is high
                        if (responseTime > 1000) {
                            builder.withDetail("warning", "High response time detected");
                        }
                        
                        return builder.build();
                    }
                }
            }
            
            return Health.down()
                .withDetail("database", "Query execution failed")
                .build();
                
        }
    }
}