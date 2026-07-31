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

/** 对应 backlog Item 1：登录校验、失败5次后锁定账号 */
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
                        .findByEmail(request.email())
                        .orElseThrow(
                                () -> new InvalidCredentialsException("邮箱或密码不正确"));

        if (!user.isEnabled()) {
            throw new AccountLockedException("账号已被禁用，请联系管理员");
        }
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new AccountLockedException("登录失败次数过多，账号已被锁定");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new InvalidCredentialsException("邮箱或密码不正确");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name(), user.getFullName());
    }
}
