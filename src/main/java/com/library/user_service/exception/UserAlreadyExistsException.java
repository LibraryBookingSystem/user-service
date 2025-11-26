package com.library.user_service.exception;

/**
 * Exception thrown when trying to register a user that already exists
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

