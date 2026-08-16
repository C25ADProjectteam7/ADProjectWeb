package com.expensehub.webbackend.entity;

import com.expensehub.webbackend.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks the approval workflow state for expense reimbursements.
 * Determines whether an expense needs manager approval before finance review.
 */
@Entity
@Table(
    name = "expense_approval_workflow",
    indexes = {
        @Index(name = "idx_expense_id", columnList = "expense_id", unique = true),
        @Index(name = "idx_department_needs_manager", columnList = "department, needs_manager_approval, manager_approved"),
        @Index(name = "idx_ready_for_finance", columnList = "ready_for_finance"),
        @Index(name = "idx_mobile_trip_id", columnList = "mobile_trip_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false, unique = true)
    private Long expenseId;

    @Column(name = "mobile_trip_id")
    private Long mobileTripId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "department", nullable = false, columnDefinition = "TEXT")
    private String department;

    @Column(name = "needs_manager_approval", nullable = false)
    private Boolean needsManagerApproval;

    @Column(name = "manager_approved")
    private Boolean managerApproved;

    @Column(name = "manager_approver_id")
    private Long managerApproverId;

    @Column(name = "manager_approved_at")
    private Instant managerApprovedAt;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "manager_note", columnDefinition = "TEXT")
    private String managerNote;

    @Column(name = "ready_for_finance", nullable = false)
    private Boolean readyForFinance;

    @Column(name = "over_budget_amount", precision = 14, scale = 2)
    private BigDecimal overBudgetAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        computeReadyForFinance();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        computeReadyForFinance();
    }

    /**
     * Computes whether the expense is ready for finance review.
     * An expense is ready for finance if either:
     * 1. It doesn't need manager approval, OR
     * 2. It has been approved by a manager
     */
    public void computeReadyForFinance() {
        this.readyForFinance = !needsManagerApproval || Boolean.TRUE.equals(managerApproved);
    }
}