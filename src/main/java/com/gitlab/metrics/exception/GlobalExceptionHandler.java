package com.gitlab.metrics.exception;

import com.gitlab.metrics.dto.ErrorResponse;
import com.gitlab.metrics.dto.webhook.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global exception handler for the application
 * Provides comprehensive error handling with detailed logging and structured error responses
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles webhook validation exceptions
     */
    @ExceptionHandler(WebhookValidationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<WebhookResponse> handleWebhookValidation(WebhookValidationException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Webhook validation failed", e, request);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(WebhookResponse.error("Webhook validation failed: " + e.getMessage()));
    }
    
    /**
     * Handles webhook processing exceptions
     */
    @ExceptionHandler(WebhookProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WebhookResponse> handleWebhookProcessing(WebhookProcessingException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Webhook processing failed", e, request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(WebhookResponse.error("Webhook processing failed: " + e.getMessage()));
    }
    
    /**
     * Handles data processing exceptions
     */
    @ExceptionHandler(DataProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDataProcessing(DataProcessingException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Data processing failed", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "DATA_PROCESSING_ERROR",
            "Data processing failed",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handles rate limit exceeded exceptions
     */
    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Rate limit exceeded", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RATE_LIMIT_EXCEEDED",
            "Rate limit exceeded. Please try again later.",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Handles external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "External service error: " + e.getServiceName(), e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "EXTERNAL_SERVICE_ERROR",
            "External service temporarily unavailable: " + e.getServiceName(),
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Handles validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Validation failed", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            request.getRequestURI(),
            e.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles method argument validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Method argument validation failed", e, request);
        
        List<String> validationErrors = new ArrayList<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            validationErrors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        errorResponse.setValidationErrors(validationErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles bind exceptions
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Bind exception", e, request);
        
        List<String> validationErrors = new ArrayList<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            validationErrors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request binding failed",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        errorResponse.setValidationErrors(validationErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Missing request parameter", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MISSING_PARAMETER",
            "Required parameter is missing: " + e.getParameterName(),
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Method argument type mismatch", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TYPE_MISMATCH",
            "Invalid parameter type for: " + e.getName(),
            request.getRequestURI(),
            "Expected type: " + e.getRequiredType().getSimpleName() + ", Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles HTTP message not readable exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "HTTP message not readable", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Malformed JSON request",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles HTTP request method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "HTTP method not supported", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "METHOD_NOT_ALLOWED",
            "HTTP method not supported: " + e.getMethod(),
            request.getRequestURI(),
            "Supported methods: " + String.join(", ", e.getSupportedMethods()) + ", Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    /**
     * Handles authentication exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Authentication failed", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "AUTHENTICATION_FAILED",
            "Authentication failed",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handles access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logWarn(errorId, "Access denied", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "Access denied",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handles database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Database access error", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "DATABASE_ERROR",
            "Database operation failed",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handles general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Unexpected runtime error", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "Internal server error",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handles general exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
        String errorId = generateErrorId();
        logError(errorId, "Unexpected error", e, request);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "Internal server error",
            request.getRequestURI(),
            "Error ID: " + errorId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Generates a unique error ID for tracking
     */
    private String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Logs error with detailed context information
     */
    private void logError(String errorId, String message, Exception e, HttpServletRequest request) {
        try {
            MDC.put("errorId", errorId);
            MDC.put("requestUri", request.getRequestURI());
            MDC.put("requestMethod", request.getMethod());
            MDC.put("remoteAddr", request.getRemoteAddr());
            MDC.put("userAgent", request.getHeader("User-Agent"));
            
            logger.error("{} - Error ID: {}, URI: {}, Method: {}, Remote: {}", 
                message, errorId, request.getRequestURI(), request.getMethod(), request.getRemoteAddr(), e);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Logs warning with detailed context information
     */
    private void logWarn(String errorId, String message, Exception e, HttpServletRequest request) {
        try {
            MDC.put("errorId", errorId);
            MDC.put("requestUri", request.getRequestURI());
            MDC.put("requestMethod", request.getMethod());
            MDC.put("remoteAddr", request.getRemoteAddr());
            
            logger.warn("{} - Error ID: {}, URI: {}, Method: {}, Remote: {}, Message: {}", 
                message, errorId, request.getRequestURI(), request.getMethod(), request.getRemoteAddr(), e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}