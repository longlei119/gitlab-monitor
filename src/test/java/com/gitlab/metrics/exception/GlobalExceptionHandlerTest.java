package com.gitlab.metrics.exception;

import com.gitlab.metrics.dto.ErrorResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for GlobalExceptionHandler
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalExceptionHandlerTest {
    
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private BindingResult bindingResult;
    
    @Before
    public void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
    }
    
    @Test
    public void testHandleDataProcessingException() {
        // Given
        DataProcessingException exception = new DataProcessingException("Data processing failed");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataProcessing(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATA_PROCESSING_ERROR", response.getBody().getErrorCode());
        assertEquals("Data processing failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertNotNull(response.getBody().getTimestamp());
    }
    
    @Test
    public void testHandleRateLimitExceededException() {
        // Given
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRateLimit(exception, request);
        
        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().getErrorCode());
        assertEquals("Rate limit exceeded. Please try again later.", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }
    
    @Test
    public void testHandleExternalServiceException() {
        // Given
        ExternalServiceException exception = new ExternalServiceException("SonarQube", "Service unavailable");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleExternalService(exception, request);
        
        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EXTERNAL_SERVICE_ERROR", response.getBody().getErrorCode());
        assertEquals("External service temporarily unavailable: SonarQube", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }
    
    @Test
    public void testHandleValidationException() {
        // Given
        ValidationException exception = new ValidationException("Invalid input data");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertEquals("Invalid input data", response.getBody().getDetails());
    }
    
    @Test
    public void testHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError1 = new FieldError("testObject", "field1", "Field1 is required");
        FieldError fieldError2 = new FieldError("testObject", "field2", "Field2 must be positive");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals("Request validation failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals(2, response.getBody().getValidationErrors().size());
        assertTrue(response.getBody().getValidationErrors().contains("field1: Field1 is required"));
        assertTrue(response.getBody().getValidationErrors().contains("field2: Field2 must be positive"));
    }
    
    @Test
    public void testHandleRuntimeException() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected runtime error");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertTrue(response.getBody().getDetails().contains("Error ID:"));
    }
    
    @Test
    public void testHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic error");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertTrue(response.getBody().getDetails().contains("Error ID:"));
    }
}