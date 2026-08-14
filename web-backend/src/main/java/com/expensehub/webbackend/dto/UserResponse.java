package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;

/**
 * user API Response DTO.
 * Do not return passwordHash to avoid exposing password hashes to the frontend.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String department,
        Role role,
        boolean enabled,
        int failedLoginAttempts
) {

    /**
     * Converts a User entity to a UserResponse DTO.
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                user.getRole(),
                user.isEnabled(),
                user.getFailedLoginAttempts()
        );
    }
}