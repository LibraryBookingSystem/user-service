package com.library.user_service.service;

import com.library.user_service.dto.*;
import com.library.user_service.entity.User;
import com.library.user_service.entity.UserRole;
import com.library.user_service.exception.InvalidCredentialsException;
import com.library.user_service.exception.UserAlreadyExistsException;
import com.library.user_service.exception.UserNotFoundException;
import com.library.user_service.repository.UserRepository;
import com.library.user_service.security.JwtUtil;
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
    private final JwtUtil jwtUtil;
    
    // @Autowired is optional when there's only one constructor
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUsername());
        
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
        
        // Save user to database
        user = userRepository.save(user);
        logger.info("User registered successfully: {}", user.getUsername());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(
            user.getUsername(), 
            user.getRole().name(), 
            user.getId()
        );
        
        // Return response with token and user info
        return new AuthResponse(token, UserResponse.fromUser(user));
    }
    
    /**
     * Login user
     */
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());
        
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
        
        logger.info("User logged in successfully: {}", user.getUsername());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(
            user.getUsername(), 
            user.getRole().name(), 
            user.getId()
        );
        
        return new AuthResponse(token, UserResponse.fromUser(user));
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
}

