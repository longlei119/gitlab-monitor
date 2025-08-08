package com.gitlab.metrics.exception;

/**
 * Exception thrown when data processing fails
 */
public class DataProcessingException extends RuntimeException {
    
    public DataProcessingException(String message) {
        super(message);
    }
    
    public DataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}