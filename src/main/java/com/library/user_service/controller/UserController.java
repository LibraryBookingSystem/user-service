package com.library.user_service.controller;

import com.library.user_service.dto.UserResponse;
import com.library.user_service.entity.UserRole;
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
    
    /**
     * Get pending users (FACULTY/ADMIN only)
     * GET /api/users/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<UserResponse>> getPendingUsers(
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {
        if (userRoleStr == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            UserRole requesterRole = UserRole.valueOf(userRoleStr.toUpperCase());
            List<UserResponse> pendingUsers = userService.getPendingUsers(requesterRole);
            return ResponseEntity.ok(pendingUsers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
    
    /**
     * Get rejected users (FACULTY/ADMIN only)
     * GET /api/users/rejected
     */
    @GetMapping("/rejected")
    public ResponseEntity<List<UserResponse>> getRejectedUsers(
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {
        if (userRoleStr == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            UserRole requesterRole = UserRole.valueOf(userRoleStr.toUpperCase());
            List<UserResponse> rejectedUsers = userService.getRejectedUsers(requesterRole);
            return ResponseEntity.ok(rejectedUsers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
    
    /**
     * Approve a user (works for both pending and rejected users)
     * POST /api/users/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String approverRoleStr) {
        if (approverRoleStr == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            UserRole approverRole = UserRole.valueOf(approverRoleStr.toUpperCase());
            UserResponse approvedUser = userService.approveUser(id, approverRole);
            return ResponseEntity.ok(approvedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
    
    /**
     * Reject a user
     * POST /api/users/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String approverRoleStr) {
        if (approverRoleStr == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            UserRole approverRole = UserRole.valueOf(approverRoleStr.toUpperCase());
            UserResponse rejectedUser = userService.rejectUser(id, approverRole);
            return ResponseEntity.ok(rejectedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
    /**
     * Delete a user (admin only)
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

