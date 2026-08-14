package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.Role;
import com.expensehub.webbackend.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByRole(Role role);
}