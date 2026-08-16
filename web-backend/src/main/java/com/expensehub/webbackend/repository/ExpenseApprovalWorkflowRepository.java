package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ExpenseApprovalWorkflowRepository extends JpaRepository<ExpenseApprovalWorkflow, Long> {

    Optional<ExpenseApprovalWorkflow> findByExpenseId(Long expenseId);

    // Find pending approvals needing manager approval
    Page<ExpenseApprovalWorkflow> findByNeedsManagerApprovalAndManagerApprovedIsNull(
        Boolean needsManagerApproval, Pageable pageable);

    // Find pending approvals for a specific department
    Page<ExpenseApprovalWorkflow> findByDepartmentAndNeedsManagerApprovalAndManagerApprovedIsNull(
        String department, Boolean needsManagerApproval, Pageable pageable);

    // Find expenses ready for finance review
    List<ExpenseApprovalWorkflow> findByReadyForFinance(Boolean readyForFinance);

    // Find approvals where manager has made a decision
    Page<ExpenseApprovalWorkflow> findByManagerApprovedIsNotNull(Pageable pageable);

    // Find workflows for multiple expense IDs (for reconciliation)
    List<ExpenseApprovalWorkflow> findByExpenseIdIn(List<Long> expenseIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExpenseApprovalWorkflow w WHERE w.expenseId = :expenseId")
    int deleteByExpenseId(@Param("expenseId") Long expenseId);

    // Optional: Find by department and ready for finance status
    Page<ExpenseApprovalWorkflow> findByDepartmentAndReadyForFinance(
        String department, Boolean readyForFinance, Pageable pageable);
}