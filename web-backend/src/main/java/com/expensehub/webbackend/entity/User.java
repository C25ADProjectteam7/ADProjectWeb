package com.expensehub.webbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Corresponding to backlog Item 1、2：Account creation and administrator account functionality
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    //Whether the account is enabled
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    //Continuous login failure count, corresponding to Item 1: Account lockout strategy
    @Builder.Default
    private int failedLoginAttempts = 0;
}
