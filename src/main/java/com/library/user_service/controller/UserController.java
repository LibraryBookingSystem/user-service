package com.library.user_service.controller;

import com.library.common.security.annotation.RequiresOwnership;
import com.library.common.security.annotation.RequiresRole;
import com.library.user_service.dto.UserResponse;
import com.library.user_service.entity.UserRole;
import com.library.user_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for user management endpoints
 * Uses AOP annotations for RBAC authorization
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current authenticated user
     * GET /api/users/me
     * Authorization: AUTHENTICATED (Any role)
     */
    @GetMapping("/me")
    @RequiresRole // Any authenticated user
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     * Authorization: AUTHENTICATED
     * Resource Ownership: Users can view their own profile, Admins can view any
     * profile
     */
    @GetMapping("/{id}")
    @RequiresRole
    @RequiresOwnership(resourceIdParam = "id", byUserId = true)
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username
     * GET /api/users/username/{username}
     * Authorization: AUTHENTICATED
     * Resource Ownership: Staff can search for any user
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Search users by partial username
     * GET /api/users/search?q=username
     * Authorization: FACULTY/ADMIN
     */
    @GetMapping("/search")
    @RequiresRole({ "FACULTY", "ADMIN" })
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String q) {
        List<UserResponse> users = userService.searchUsersByUsername(q);
        return ResponseEntity.ok(users);
    }

    /**
     * Get all users
     * GET /api/users
     * Authorization: ADMIN only
     */
    @GetMapping
    @RequiresRole({ "ADMIN" })
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Restrict a user
     * POST /api/users/{id}/restrict
     * Authorization: ADMIN only
     */
    @PostMapping("/{id}/restrict")
    @RequiresRole({ "ADMIN" })
    public ResponseEntity<UserResponse> restrictUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        UserResponse user = userService.restrictUser(id, reason);
        return ResponseEntity.ok(user);
    }

    /**
     * Unrestrict a user
     * POST /api/users/{id}/unrestrict
     * Authorization: ADMIN only
     */
    @PostMapping("/{id}/unrestrict")
    @RequiresRole({ "ADMIN" })
    public ResponseEntity<UserResponse> unrestrictUser(@PathVariable Long id) {
        UserResponse user = userService.unrestrictUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Check if user is restricted
     * GET /api/users/{id}/restricted
     * Authorization: AUTHENTICATED
     * Resource Ownership: Users can check their own status, Admins can check any
     */
    @GetMapping("/{id}/restricted")
    @RequiresRole
    @RequiresOwnership(resourceIdParam = "id", byUserId = true)
    public ResponseEntity<Map<String, Boolean>> isUserRestricted(@PathVariable Long id) {
        boolean restricted = userService.isUserRestricted(id);
        return ResponseEntity.ok(Map.of("restricted", restricted));
    }

    /**
     * Get pending users
     * GET /api/users/pending
     * Authorization: FACULTY/ADMIN only
     */
    @GetMapping("/pending")
    @RequiresRole({ "FACULTY", "ADMIN" })
    public ResponseEntity<List<UserResponse>> getPendingUsers(HttpServletRequest request) {
        String roleStr = (String) request.getAttribute("userRole");
        UserRole requesterRole = UserRole.valueOf(roleStr);
        List<UserResponse> pendingUsers = userService.getPendingUsers(requesterRole);
        return ResponseEntity.ok(pendingUsers);
    }

    /**
     * Get rejected users
     * GET /api/users/rejected
     * Authorization: FACULTY/ADMIN only
     */
    @GetMapping("/rejected")
    @RequiresRole({ "FACULTY", "ADMIN" })
    public ResponseEntity<List<UserResponse>> getRejectedUsers(HttpServletRequest request) {
        String roleStr = (String) request.getAttribute("userRole");
        UserRole requesterRole = UserRole.valueOf(roleStr);
        List<UserResponse> rejectedUsers = userService.getRejectedUsers(requesterRole);
        return ResponseEntity.ok(rejectedUsers);
    }

    /**
     * Approve a user
     * POST /api/users/{id}/approve
     * Authorization: FACULTY/ADMIN only
     */
    @PostMapping("/{id}/approve")
    @RequiresRole({ "FACULTY", "ADMIN" })
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        String roleStr = (String) request.getAttribute("userRole");
        UserRole approverRole = UserRole.valueOf(roleStr);
        UserResponse approvedUser = userService.approveUser(id, approverRole);
        return ResponseEntity.ok(approvedUser);
    }

    /**
     * Reject a user
     * POST /api/users/{id}/reject
     * Authorization: FACULTY/ADMIN only
     */
    @PostMapping("/{id}/reject")
    @RequiresRole({ "FACULTY", "ADMIN" })
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        String roleStr = (String) request.getAttribute("userRole");
        UserRole approverRole = UserRole.valueOf(roleStr);
        UserResponse rejectedUser = userService.rejectUser(id, approverRole);
        return ResponseEntity.ok(rejectedUser);
    }

    /**
     * Delete a user
     * DELETE /api/users/{id}
     * Authorization: ADMIN only
     */
    @DeleteMapping("/{id}")
    @RequiresRole({ "ADMIN" })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
