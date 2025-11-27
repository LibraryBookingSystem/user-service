package com.library.user_service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Internal DTO for validating user credentials (called by auth-service)
 */
public class ValidateCredentialsRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    public ValidateCredentialsRequest() {}
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

