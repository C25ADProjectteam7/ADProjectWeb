package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.dto.LoginRequest;
import com.expensehub.webbackend.dto.LoginResponse;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.exception.AccountLockedException;
import com.expensehub.webbackend.exception.InvalidCredentialsException;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.security.JwtUtil;
import com.expensehub.webbackend.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmailHash(User.sha256Hex(request.email().toLowerCase().trim()))
                        .orElseThrow(
                                () -> new InvalidCredentialsException("email or password is incorrect"));

        if (!user.isEnabled()) {
            throw new AccountLockedException("account is disabled, please contact administrator");
        }
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new AccountLockedException("too many failed login attempts, account is locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new InvalidCredentialsException("email or password is incorrect");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name(), user.getFullName());
    }
}
