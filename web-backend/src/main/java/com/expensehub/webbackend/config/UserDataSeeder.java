package com.expensehub.webbackend.config;

import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

// Only effective in dev environment to avoid affecting production.
// Credentials here must match TEST_ACCOUNTS.md — that file is what teammates
// and graders actually read, so it's the source of truth, not this class.
@Configuration
@Profile("dev")
public class UserDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return; // Existing users will be skipped to prevent duplicate insertion
            }

            userRepository.save(User.builder()
                    .email("admin@test.com")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .role(Role.ADMIN)
                    .fullName("Admin User")
                    .department("IT")
                    .enabled(true)
                    .failedLoginAttempts(0)
                    .build());

            userRepository.save(User.builder()
                    .email("finance@test.com")
                    .passwordHash(passwordEncoder.encode("Finance123!"))
                    .role(Role.FINANCE_STAFF)
                    .fullName("Finance Staff")
                    .department("Finance")
                    .enabled(true)
                    .failedLoginAttempts(0)
                    .build());

            userRepository.save(User.builder()
                    .email("manager@test.com")
                    .passwordHash(passwordEncoder.encode("Manager@123"))
                    .role(Role.MANAGER)
                    .fullName("Manager User")
                    .department("Sales")
                    .enabled(true)
                    .failedLoginAttempts(0)
                    .build());

            // Emails only — printing the passwords put them in stdout, which
            // means `docker compose logs` and any log shipper downstream.
            // The passwords are in TEST_ACCOUNTS.md for whoever needs them.
            log.info("Seeded dev test users (passwords in TEST_ACCOUNTS.md): {}, {}, {}",
                    "admin@test.com", "finance@test.com", "manager@test.com");
        };
    }
}
