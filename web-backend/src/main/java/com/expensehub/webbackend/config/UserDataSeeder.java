package com.expensehub.webbackend.config;

import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")   // Only effective in dev environment to avoid affecting production
public class UserDataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return; // Existing users will be skipped to prevent duplicate insertion
            }

            userRepository.save(User.builder()
                    .email("admin@expensehub.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .fullName("Admin User")
                    .department("IT")
                    .enabled(true)
                    .failedLoginAttempts(0)
                    .build());

            userRepository.save(User.builder()
                    .email("finance@expensehub.com")
                    .passwordHash(passwordEncoder.encode("finance123"))
                    .role(Role.FINANCE_STAFF)
                    .fullName("Finance Staff")
                    .department("Finance")
                    .enabled(true)
                    .failedLoginAttempts(0)
                    .build());



            System.out.println("✅ Test Users Created:");
            System.out.println("   ADMIN: admin@expensehub.com / admin123");
            System.out.println("   FINANCE_STAFF: finance@expensehub.com / finance123");
            System.out.println("   MANAGER: manager@expensehub.com / manager123");
            System.out.println("   EMPLOYEE: employee@expensehub.com / employee123");
        };
    }
}