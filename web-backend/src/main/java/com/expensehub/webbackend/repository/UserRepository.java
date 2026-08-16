package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Lookup user by SHA-256 hash of their lowercase email.
     * The email field itself is AES-GCM encrypted and non-deterministic,
     * so direct findByEmail queries would not match.
     * The emailHash is computed automatically via @PrePersist/@PreUpdate.
     */
    Optional<User> findByEmailHash(String emailHash);

    long countByRole(Role role);
}