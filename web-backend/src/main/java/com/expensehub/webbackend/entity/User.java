package com.expensehub.webbackend.entity;

import com.expensehub.webbackend.security.EncryptedStringConverter;
import com.expensehub.webbackend.security.HashUtil;
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

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String email;

    /** SHA-256 hash of lowercase email, used for indexed lookups (login etc.). */
    @Column(name = "email_hash", unique = true, nullable = false, length = 64)
    private String emailHash;

    @Column(nullable = false)
    private String passwordHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String fullName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
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

    @PrePersist
    @PreUpdate
    public void updateEmailHash() {
        if (this.email != null) {
            this.emailHash = sha256Hex(this.email.toLowerCase().trim());
        }
    }

    public static String sha256Hex(String value) {
        return HashUtil.sha256Hex(value);
    }
}
