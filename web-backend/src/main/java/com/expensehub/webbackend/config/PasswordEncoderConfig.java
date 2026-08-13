package com.expensehub.webbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Minimal replacement for SecurityConfig's PasswordEncoder bean (that class
 * was intentionally omitted for now — see project notes on step-by-step
 * verification — so AuthServiceImpl would otherwise fail to start).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}