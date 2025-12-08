package com.library.user_service.exception;

/**
 * Exception thrown when user doesn't have permission to perform an action
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
}

