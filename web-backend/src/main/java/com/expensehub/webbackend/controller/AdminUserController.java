package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.dto.CreateUserRequest;
import com.expensehub.webbackend.dto.UpdateUserRequest;
import com.expensehub.webbackend.dto.UserResponse;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

// Corresponding to backlog Item 2: Administrator account functions (viewing, creating, editing, disabling, and enabling other accounts).
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Admin: view all users
     */
    @GetMapping
    public List<UserResponse> listUsers() {
        return adminUserService.listUsers()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Admin: create a new user
     */
    @PostMapping
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request) {

        User user = adminUserService.createUser(request);
        return UserResponse.from(user);
    }

    /**
     * Admin: update user information.
     * Password is optional.
     */
    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        User user = adminUserService.updateUser(id, request);
        return UserResponse.from(user);
    }

    /**
     * Admin: enable or disable a user.
     */
    @PatchMapping("/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        User user = adminUserService.updateUserStatus(id, enabled);
        return UserResponse.from(user);
    }
}