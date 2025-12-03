package com.library.user_service.controller;

import com.library.user_service.dto.UserResponse;
import com.library.user_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for user management endpoints
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    // @Autowired is optional when there's only one constructor
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get current authenticated user
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get user by username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get all users (admin only in production)
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Restrict a user (admin only)
     * POST /api/users/{id}/restrict
     */
    @PostMapping("/{id}/restrict")
    public ResponseEntity<UserResponse> restrictUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        UserResponse user = userService.restrictUser(id, reason);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Unrestrict a user (admin only)
     * POST /api/users/{id}/unrestrict
     */
    @PostMapping("/{id}/unrestrict")
    public ResponseEntity<UserResponse> unrestrictUser(@PathVariable Long id) {
        UserResponse user = userService.unrestrictUser(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Check if user is restricted (for other services)
     * GET /api/users/{id}/restricted
     */
    @GetMapping("/{id}/restricted")
    public ResponseEntity<Map<String, Boolean>> isUserRestricted(@PathVariable Long id) {
        boolean restricted = userService.isUserRestricted(id);
        return ResponseEntity.ok(Map.of("restricted", restricted));
    }
}

