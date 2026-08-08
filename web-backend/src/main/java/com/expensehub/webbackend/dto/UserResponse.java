package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;

/**
 * 用户信息的 API 返回对象。
 *
 * 注意：
 * 不返回 passwordHash，避免密码哈希泄露给前端。
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
     * 将 User Entity 转换成 API Response。
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