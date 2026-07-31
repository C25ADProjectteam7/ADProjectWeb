package com.expensehub.webbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 对应 backlog Item 1、2：账号创建与管理员账号功能 */
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

    /** 账号是否启用，对应 Item 2：禁用/启用其他账号 */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** 连续登录失败次数，对应 Item 1：账号锁定策略 */
    @Builder.Default
    private int failedLoginAttempts = 0;
}
