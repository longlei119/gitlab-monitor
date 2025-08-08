package com.gitlab.metrics.exception;

/**
 * Exception thrown when external service calls fail
 */
public class ExternalServiceException extends RuntimeException {
    
    private final String serviceName;
    
    public ExternalServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}