package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import com.expensehub.webbackend.entity.ReimbursementAuditLog;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.repository.ExpenseApprovalWorkflowRepository;
import com.expensehub.webbackend.repository.ReimbursementAuditLogRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for manager approval of expense reimbursements.
 * Handles the business logic for managers reviewing and deciding on expenses
 * that exceed department budgets.
 */
@Service
@Transactional
public class ManagerExpenseService {

    private final ExpenseApprovalWorkflowService workflowService;
    private final ExpenseApprovalWorkflowRepository workflowRepository;
    private final MobileExpenseClient mobileExpenseClient;
    private final ReimbursementAuditLogRepository auditLogRepository;

    public ManagerExpenseService(
        ExpenseApprovalWorkflowService workflowService,
        ExpenseApprovalWorkflowRepository workflowRepository,
        MobileExpenseClient mobileExpenseClient,
        ReimbursementAuditLogRepository auditLogRepository
    ) {
        this.workflowService = workflowService;
        this.workflowRepository = workflowRepository;
        this.mobileExpenseClient = mobileExpenseClient;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get pending expense approvals needing manager decision.
     * Role-based: any manager can approve any expense regardless of department.
     */
    public Page<ExpenseApprovalWorkflow> listPendingApprovals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Find all expenses needing manager approval that haven't been decided yet
        return workflowRepository.findByNeedsManagerApprovalAndManagerApprovedIsNull(
            Boolean.TRUE, pageable);
    }

    /**
     * Get pending expense approvals for a specific department.
     * Optional filtering for managers who only want to see their department's expenses.
     */
    public Page<ExpenseApprovalWorkflow> listPendingApprovalsByDepartment(
        String department, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return workflowRepository.findByDepartmentAndNeedsManagerApprovalAndManagerApprovedIsNull(
            department, Boolean.TRUE, pageable);
    }

    /**
     * Approve an expense as a manager.
     * Updates workflow state but doesn't change Mobile backend status yet.
     * Finance will review and make final decision.
     */
    public void approveExpense(Long expenseId, Long managerId, String note) {
        workflowService.updateManagerApproval(expenseId, managerId, note);
        logAudit(expenseId, "MANAGER_APPROVE", managerId, note);
    }

    /**
     * Reject an expense as a manager.
     * Updates Mobile backend to REJECTED status and clears workflow.
     */
    public void rejectExpense(Long expenseId, Long managerId, String note) {
        // Update Mobile backend via API to reject the expense
        try {
            mobileExpenseClient.reject(expenseId, note);
        } catch (Exception e) {
            // Log error but continue with workflow update
            // The expense might already be rejected or not found
        }

        // Clear workflow since expense is rejected
        workflowService.clearWorkflowOnRejection(expenseId);
        logAudit(expenseId, "MANAGER_REJECT", managerId, note);
    }

    /**
     * Get approval history for expenses (approved or rejected by managers).
     */
    public Page<ExpenseApprovalWorkflow> listApprovalHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "managerApprovedAt"));

        // Find expenses where manager approval decision has been made
        return workflowRepository.findByManagerApprovedIsNotNull(pageable);
    }

    /**
     * Check if a manager has already decided on an expense.
     */
    public boolean hasManagerDecided(Long expenseId) {
        return workflowRepository.findByExpenseId(expenseId)
            .map(workflow -> workflow.getManagerApproved() != null)
            .orElse(false);
    }

    /**
     * Get the manager's decision for an expense.
     */
    public Boolean getManagerDecision(Long expenseId) {
        return workflowRepository.findByExpenseId(expenseId)
            .map(ExpenseApprovalWorkflow::getManagerApproved)
            .orElse(null);
    }

    /**
     * Log audit entry for manager action on expense reimbursement.
     */
    private void logAudit(Long expenseId, String action, Long managerId, String note) {
        ReimbursementAuditLog log = ReimbursementAuditLog.builder()
            .requestId(expenseId)
            .action(action)
            .detail("managerId=" + managerId
                + (note != null && !note.isBlank() ? "; note: " + note : ""))
            .changedBy("manager#" + managerId)
            .changedAt(Instant.now())
            .build();
        auditLogRepository.save(log);
    }
}