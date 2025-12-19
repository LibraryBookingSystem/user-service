package com.library.user_service.service;

import com.library.user_service.dto.*;
import com.library.user_service.entity.User;
import com.library.user_service.entity.UserRole;
import com.library.common.exception.ForbiddenException;
import com.library.user_service.exception.InvalidCredentialsException;
import com.library.user_service.exception.UserAlreadyExistsException;
import com.library.user_service.exception.UserNotFoundException;
import com.library.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for user operations
 * Contains the business logic
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user (internal - called by auth-service)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Set role (default to STUDENT if not specified)
        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            user.setRole(UserRole.STUDENT);
        }

        // Set pending approval based on role
        // STUDENT accounts are auto-approved, FACULTY/ADMIN require approval
        if (user.getRole() == UserRole.STUDENT) {
            user.setPendingApproval(false);
        } else {
            user.setPendingApproval(true);
        }
        user.setRejected(false);

        // Save user to database
        user = userRepository.save(user);
        logger.info("User created successfully: {} with role: {} (pendingApproval: {})",
                user.getUsername(), user.getRole(), user.isPendingApproval());

        return UserResponse.fromUser(user);
    }

    /**
     * Validate user credentials (internal - called by auth-service)
     */
    public UserResponse validateCredentials(ValidateCredentialsRequest request) {
        logger.info("Validating credentials for user: {}", request.getUsername());

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Check if password matches
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Check if user is restricted
        if (user.isRestricted()) {
            throw new InvalidCredentialsException("Account is restricted: " + user.getRestrictionReason());
        }

        // Check if user is pending approval
        if (user.isPendingApproval()) {
            throw new InvalidCredentialsException(
                    "Your account is pending approval. Please wait for an administrator or faculty member to approve your registration.");
        }

        // Check if user is rejected
        if (user.isRejected()) {
            throw new InvalidCredentialsException(
                    "Your account registration was rejected. Please contact an administrator.");
        }

        logger.info("Credentials validated successfully for user: {}", user.getUsername());

        return UserResponse.fromUser(user);
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return UserResponse.fromUser(user);
    }

    /**
     * Get user by username
     */
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return UserResponse.fromUser(user);
    }

    /**
     * Get all users (for admin)
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Search users by partial username (for staff to search users)
     */
    public List<UserResponse> searchUsersByUsername(String query) {
        logger.info("Searching users by username containing: {}", query);
        return userRepository.findByUsernameContainingIgnoreCase(query).stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Restrict a user (for admin)
     */
    @Transactional
    public UserResponse restrictUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setRestricted(true);
        user.setRestrictionReason(reason);
        user = userRepository.save(user);

        logger.info("User restricted: {} - Reason: {}", user.getUsername(), reason);
        return UserResponse.fromUser(user);
    }

    /**
     * Unrestrict a user (for admin)
     */
    @Transactional
    public UserResponse unrestrictUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setRestricted(false);
        user.setRestrictionReason(null);
        user = userRepository.save(user);

        logger.info("User unrestricted: {}", user.getUsername());
        return UserResponse.fromUser(user);
    }

    /**
     * Check if user is restricted (for other services to call)
     */
    public boolean isUserRestricted(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return user.isRestricted();
    }

    /**
     * Approve a user (works for both pending and rejected users)
     */
    @Transactional
    public UserResponse approveUser(Long userId, UserRole approverRole) {
        logger.info("Approving user {} by approver with role {}", userId, approverRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Check if user is pending approval or rejected
        if (!user.isPendingApproval() && !user.isRejected()) {
            throw new IllegalArgumentException("User is not pending approval or rejected");
        }

        // Validate approver permissions
        if (approverRole == UserRole.FACULTY) {
            // FACULTY can only approve FACULTY users
            if (user.getRole() != UserRole.FACULTY) {
                throw new ForbiddenException("FACULTY members can only approve other FACULTY members");
            }
        } else if (approverRole == UserRole.ADMIN) {
            // ADMIN can approve FACULTY and ADMIN users
            if (user.getRole() != UserRole.FACULTY && user.getRole() != UserRole.ADMIN) {
                throw new ForbiddenException("ADMIN can only approve FACULTY and ADMIN members");
            }
        } else {
            throw new ForbiddenException("Only FACULTY and ADMIN members can approve users");
        }

        // Approve the user
        user.setPendingApproval(false);
        user.setRejected(false);
        user = userRepository.save(user);

        logger.info("User {} approved successfully by {}", user.getUsername(), approverRole);
        return UserResponse.fromUser(user);
    }

    /**
     * Reject a user
     */
    @Transactional
    public UserResponse rejectUser(Long userId, UserRole approverRole) {
        logger.info("Rejecting user {} by approver with role {}", userId, approverRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Validate approver permissions (same as approve)
        if (approverRole == UserRole.FACULTY) {
            // FACULTY can only reject FACULTY users
            if (user.getRole() != UserRole.FACULTY) {
                throw new ForbiddenException("FACULTY members can only reject other FACULTY members");
            }
        } else if (approverRole == UserRole.ADMIN) {
            // ADMIN can reject FACULTY and ADMIN users
            if (user.getRole() != UserRole.FACULTY && user.getRole() != UserRole.ADMIN) {
                throw new ForbiddenException("ADMIN can only reject FACULTY and ADMIN members");
            }
        } else {
            throw new ForbiddenException("Only FACULTY and ADMIN members can reject users");
        }

        // Reject the user
        user.setRejected(true);
        user.setPendingApproval(false);
        user = userRepository.save(user);

        logger.info("User {} rejected by {}", user.getUsername(), approverRole);
        return UserResponse.fromUser(user);
    }

    /**
     * Get pending users (filtered by requester role)
     */
    public List<UserResponse> getPendingUsers(UserRole requesterRole) {
        logger.info("Getting pending users for requester with role {}", requesterRole);

        List<User> pendingUsers;

        if (requesterRole == UserRole.FACULTY) {
            // FACULTY can only see pending FACULTY users
            pendingUsers = userRepository.findByPendingApprovalTrueAndRejectedFalseAndRole(UserRole.FACULTY);
        } else if (requesterRole == UserRole.ADMIN) {
            // ADMIN can see pending FACULTY and ADMIN users
            pendingUsers = userRepository.findByPendingApprovalTrueAndRejectedFalseAndRoleIn(
                    List.of(UserRole.FACULTY, UserRole.ADMIN));
        } else {
            throw new ForbiddenException("Only FACULTY and ADMIN members can view pending users");
        }

        return pendingUsers.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Get rejected users (filtered by requester role)
     */
    public List<UserResponse> getRejectedUsers(UserRole requesterRole) {
        logger.info("Getting rejected users for requester with role {}", requesterRole);

        List<User> rejectedUsers;

        if (requesterRole == UserRole.FACULTY) {
            // FACULTY can only see rejected FACULTY users
            rejectedUsers = userRepository.findByRejectedTrueAndRole(UserRole.FACULTY);
        } else if (requesterRole == UserRole.ADMIN) {
            // ADMIN can see rejected FACULTY and ADMIN users
            rejectedUsers = userRepository.findByRejectedTrueAndRoleIn(
                    List.of(UserRole.FACULTY, UserRole.ADMIN));
        } else {
            throw new ForbiddenException("Only FACULTY and ADMIN members can view rejected users");
        }

        return rejectedUsers.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Delete a user (admin only)
     */
    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Deleting user with id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        userRepository.deleteById(userId);
        logger.info("User deleted successfully: {}", userId);
    }
}
