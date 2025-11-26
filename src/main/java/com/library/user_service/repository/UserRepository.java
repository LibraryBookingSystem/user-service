package com.library.user_service.repository;

import com.library.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity
 * JpaRepository provides basic CRUD operations automatically
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Data JPA will automatically implement these methods
    // based on the method names
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}

