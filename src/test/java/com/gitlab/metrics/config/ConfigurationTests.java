package com.gitlab.metrics.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class ConfigurationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testDatabaseConfiguration() throws Exception {
        assertNotNull(dataSource, "DataSource should not be null");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Database connection should not be null");
        }
    }

    @Test
    public void testRedisConfiguration() {
        assertNotNull(redisTemplate, "RedisTemplate should not be null");
        // Note: Actual Redis operations require a running Redis instance
        // This test only verifies that the RedisTemplate bean is properly configured
    }
}