package com.library.user_service.controller;

import com.library.user_service.dto.CreateUserRequest;
import com.library.user_service.dto.UserResponse;
import com.library.user_service.dto.ValidateCredentialsRequest;
import com.library.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal controller for inter-service communication
 * These endpoints should only be called by other services (auth-service)
 * In production, these should be secured or exposed through internal network only
 */
@RestController
@RequestMapping("/api/users/internal")
public class InternalUserController {
    
    private final UserService userService;
    
    public InternalUserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Create a new user (called by auth-service during registration)
     * POST /api/users/internal/create
     */
    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }
    
    /**
     * Validate user credentials (called by auth-service during login)
     * POST /api/users/internal/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<UserResponse> validateCredentials(@Valid @RequestBody ValidateCredentialsRequest request) {
        UserResponse user = userService.validateCredentials(request);
        return ResponseEntity.ok(user);
    }
}

