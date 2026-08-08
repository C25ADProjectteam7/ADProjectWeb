package com.expensehub.webbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Corresponds to backlog Item 20: Manager Approval Notifications.
 * The approval record lives in the Web database. It only references the Mobile
 * trip by id (mobileTripId) — there is no cross-database foreign key, since Web
 * and Mobile are two independent MySQL instances.
 * Employee/department/destination/budget fields are a snapshot taken when the
 * approval was created, so history stays readable even if the source trip in
 * Mobile's database is later changed or deleted.
 */
@Entity
@Table(name = "approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mobileTripId;

    /** Set only once a manager actually approves/rejects; null while PENDING. */
    private Long managerId;

    private String employeeName;
    private String department;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budgetRequested;
    private BigDecimal departmentBudgetLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    private String note;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;
}