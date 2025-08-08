package com.gitlab.metrics.health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for RabbitMQ connectivity
 */
@Component
public class RabbitMQHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Override
    public Health health() {
        try {
            return checkRabbitMQHealth();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("rabbitmq", "Connection failed")
                .build();
        }
    }
    
    private Health checkRabbitMQHealth() throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test connection by getting connection factory
            if (rabbitTemplate.getConnectionFactory() == null) {
                return Health.down()
                    .withDetail("rabbitmq", "Connection factory is null")
                    .build();
            }
            
            // Test by creating a connection
            rabbitTemplate.getConnectionFactory().createConnection();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return Health.up()
                .withDetail("rabbitmq", "UP")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("connectionFactory", rabbitTemplate.getConnectionFactory().getClass().getSimpleName())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("rabbitmq", "Connection test failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}