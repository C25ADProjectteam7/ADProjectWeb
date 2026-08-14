package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.dto.CreateUserRequest;
import com.expensehub.webbackend.dto.UpdateUserRequest;
import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.service.AdminUserService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        User user = User.builder()
                .email(request.email().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .department(request.department())
                .role(request.role())
                .enabled(true)
                .failedLoginAttempts(0)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"));

        /*
         * Prevent the system from losing its last administrator.
         *
         * If this user is currently an ADMIN and the requested role
         * changes to another role, make sure another ADMIN exists.
         */
        if (request.role() != null
                && user.getRole() == Role.ADMIN
                && request.role() != Role.ADMIN) {

            ensureAnotherAdminExists(user.getId());
        }

        if (request.fullName() != null
                && !request.fullName().isBlank()) {

            user.setFullName(request.fullName().trim());
        }

        if (request.department() != null) {
            user.setDepartment(request.department().trim());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.password() != null
                && !request.password().isBlank()) {

            user.setPasswordHash(
                    passwordEncoder.encode(request.password()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserStatus(Long id, boolean enabled) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"));

        /*
         * Do not allow disabling the last administrator.
         *
         * This protects the system from ending up with no
         * administrator account.
         */
        if (!enabled
                && user.getRole() == Role.ADMIN) {

            ensureAnotherAdminExists(user.getId());
        }

        user.setEnabled(enabled);

        /*
         * Re-enabling an account should also clear its failed-login
         * counter. AuthServiceImpl locks login on two independent
         * conditions (enabled == false, OR failedLoginAttempts >= 5) —
         * without this, an admin re-enabling a locked-out account would
         * still see it rejected at login by the second condition.
         */
        if (enabled) {
            user.setFailedLoginAttempts(0);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User unlockUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"));

        user.setFailedLoginAttempts(0);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    /**
     * Make sure there is at least one other ADMIN account.
     */
    private void ensureAnotherAdminExists(Long currentUserId) {

        long adminCount = userRepository.countByRole(Role.ADMIN);

        if (adminCount <= 1) {
            throw new IllegalStateException(
                    "The last administrator account cannot be disabled or downgraded");
        }
    }
}