package com.gitlab.metrics.health;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for DatabaseHealthIndicator
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseHealthIndicatorTest {
    
    @InjectMocks
    private DatabaseHealthIndicator databaseHealthIndicator;
    
    @Mock
    private DataSource dataSource;
    
    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;
    
    @Before
    public void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }
    
    @Test
    public void testHealthCheckSuccess() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(resultSet.next()).thenReturn(true);
        
        // When
        Health health = databaseHealthIndicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertEquals("UP", health.getDetails().get("database"));
        assertNotNull(health.getDetails().get("responseTime"));
        assertEquals("SELECT 1", health.getDetails().get("validationQuery"));
    }
    
    @Test
    public void testHealthCheckConnectionInvalid() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(false);
        
        // When
        Health health = databaseHealthIndicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Connection validation failed", health.getDetails().get("database"));
    }
    
    @Test
    public void testHealthCheckQueryFailed() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(resultSet.next()).thenReturn(false);
        
        // When
        Health health = databaseHealthIndicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Query execution failed", health.getDetails().get("database"));
    }
    
    @Test
    public void testHealthCheckConnectionException() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
        
        // When
        Health health = databaseHealthIndicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Connection failed", health.getDetails().get("error"));
        assertEquals("Connection failed", health.getDetails().get("database"));
    }
    
    @Test
    public void testHealthCheckHighResponseTime() throws SQLException {
        // Given
        when(connection.isValid(anyInt())).thenReturn(true);
        when(resultSet.next()).thenAnswer(invocation -> {
            // Simulate slow query
            Thread.sleep(1100);
            return true;
        });
        
        // When
        Health health = databaseHealthIndicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertEquals("UP", health.getDetails().get("database"));
        assertNotNull(health.getDetails().get("warning"));
        assertEquals("High response time detected", health.getDetails().get("warning"));
    }
}