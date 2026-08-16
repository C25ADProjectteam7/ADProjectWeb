package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import com.expensehub.webbackend.repository.ExpenseApprovalWorkflowRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing expense approval workflow state.
 * Determines whether expenses need manager approval based on budget status.
 */
@Service
@Transactional
public class ExpenseApprovalWorkflowService {

    private final ExpenseApprovalWorkflowRepository workflowRepository;
    private final BudgetConfigRepository budgetConfigRepository;
    private final ReimbursementPolicyEngine policyEngine;

    public ExpenseApprovalWorkflowService(
        ExpenseApprovalWorkflowRepository workflowRepository,
        BudgetConfigRepository budgetConfigRepository,
        ReimbursementPolicyEngine policyEngine
    ) {
        this.workflowRepository = workflowRepository;
        this.budgetConfigRepository = budgetConfigRepository;
        this.policyEngine = policyEngine;
    }

    /**
     * Initialize workflow state for a new expense.
     */
    public ExpenseApprovalWorkflow initializeWorkflowForExpense(
        MobileExpenseDTO expense,
        MobileUserDTO user
    ) {
        // Calculate if needs manager approval
        boolean needsManagerApproval = calculateNeedsManagerApproval(expense, user);

        // Calculate over-budget amount for informational purposes
        BigDecimal overBudgetAmount = calculateOverBudgetAmount(expense, user);

        return createNewWorkflow(expense, user, needsManagerApproval, overBudgetAmount);
    }

    /**
     * Update existing workflow (e.g., when budgets change).
     */
    public ExpenseApprovalWorkflow updateWorkflowForExpense(
        Long expenseId,
        MobileExpenseDTO expense,
        MobileUserDTO user
    ) {
        boolean needsManagerApproval = calculateNeedsManagerApproval(expense, user);
        BigDecimal overBudgetAmount = calculateOverBudgetAmount(expense, user);

        return workflowRepository.findByExpenseId(expenseId)
            .map(existing -> updateExistingWorkflow(existing, needsManagerApproval, overBudgetAmount))
            .orElseGet(() -> createNewWorkflow(expense, user, needsManagerApproval, overBudgetAmount));
    }

    /**
     * Calculate whether an expense needs manager approval based on OVER_BUDGET flag.
     */
    private boolean calculateNeedsManagerApproval(
        MobileExpenseDTO expense,
        MobileUserDTO user
    ) {
        List<String> flags = calculateFlags(expense, user);
        return flags.contains(ReimbursementPolicyEngine.FLAG_OVER_BUDGET);
    }

    /**
     * Calculate the over-budget amount (positive if over budget, null or zero if not).
     */
    private BigDecimal calculateOverBudgetAmount(
        MobileExpenseDTO expense,
        MobileUserDTO user
    ) {
        String department = user != null ? user.getDepartment() : null;
        if (department == null) {
            return null;
        }

        LocalDate anchor = expense.getSubmittedAt() != null
            ? expense.getSubmittedAt().toLocalDate()
            : LocalDate.now();

        // Look up budget for department and period
        Optional<BudgetConfig> budget = findBudgetForDepartmentAndDate(department, anchor);

        if (budget.isPresent()) {
            BudgetConfig cfg = budget.get();
            // For simplicity, we'll just check if this expense alone exceeds budget
            // In a real implementation, we'd need to sum all expenses in the period
            BigDecimal budgetAmount = cfg.getAmount();
            BigDecimal expenseAmount = expense.getAmount();

            // This is a simplified check - actual implementation would need
            // to sum all expenses for the department in the period
            if (expenseAmount.compareTo(budgetAmount) > 0) {
                return expenseAmount.subtract(budgetAmount);
            }
        }

        return null; // Not over budget
    }

    /**
     * Calculate flags for an expense.
     */
    private List<String> calculateFlags(
        MobileExpenseDTO expense,
        MobileUserDTO user
    ) {
        String department = user != null ? user.getDepartment() : null;

        // If no department, evaluate without budget check
        if (department == null) {
            return policyEngine.evaluate(
                expense.getAmount(),
                ReimbursementCategory.valueOf(expense.getCategory()),
                expense.getReceiptUrl() != null && !expense.getReceiptUrl().isBlank(),
                Optional.empty(),
                expense.getAmount()
            );
        }

        LocalDate anchor = expense.getSubmittedAt() != null
            ? expense.getSubmittedAt().toLocalDate()
            : LocalDate.now();

        Optional<BudgetConfig> budget = findBudgetForDepartmentAndDate(department, anchor);

        // Simplified: use expense amount as period spent for flag calculation
        // Actual implementation would need to sum expenses in period
        BigDecimal periodSpent = expense.getAmount();

        return policyEngine.evaluate(
            expense.getAmount(),
            ReimbursementCategory.valueOf(expense.getCategory()),
            expense.getReceiptUrl() != null && !expense.getReceiptUrl().isBlank(),
            budget,
            periodSpent
        );
    }

    /**
     * Find budget for a department and date (quarterly first, then annual).
     */
    private Optional<BudgetConfig> findBudgetForDepartmentAndDate(String department, LocalDate date) {
        String quarterLabel = BudgetPeriodResolver.quarterlyLabel(date);
        String annualLabel = BudgetPeriodResolver.annualLabel(date);

        return budgetConfigRepository
            .findByDepartmentAndPeriodTypeAndPeriodLabel(department, BudgetPeriodType.QUARTERLY, quarterLabel)
            .or(() -> budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                department, BudgetPeriodType.ANNUAL, annualLabel));
    }

    private ExpenseApprovalWorkflow updateExistingWorkflow(
        ExpenseApprovalWorkflow existing,
        boolean needsManagerApproval,
        BigDecimal overBudgetAmount
    ) {
        // Only update if needsManagerApproval changed
        if (existing.getNeedsManagerApproval() != needsManagerApproval) {
            existing.setNeedsManagerApproval(needsManagerApproval);
            existing.setOverBudgetAmount(overBudgetAmount);

            // If needs manager approval changed to false, ensure ready for finance
            existing.computeReadyForFinance();
            return workflowRepository.save(existing);
        }

        // Update over-budget amount if provided
        if (overBudgetAmount != null && !overBudgetAmount.equals(existing.getOverBudgetAmount())) {
            existing.setOverBudgetAmount(overBudgetAmount);
            return workflowRepository.save(existing);
        }

        return existing;
    }

    private ExpenseApprovalWorkflow createNewWorkflow(
        MobileExpenseDTO expense,
        MobileUserDTO user,
        boolean needsManagerApproval,
        BigDecimal overBudgetAmount
    ) {
        ExpenseApprovalWorkflow workflow = new ExpenseApprovalWorkflow();
        workflow.setExpenseId(expense.getId());
        workflow.setMobileTripId(expense.getTripId());
        workflow.setDepartment(
            user != null && user.getDepartment() != null
                ? user.getDepartment()
                : "Unknown"
        );
        workflow.setNeedsManagerApproval(needsManagerApproval);
        workflow.setManagerApproved(null);
        workflow.setManagerApproverId(null);
        workflow.setManagerApprovedAt(null);
        workflow.setManagerNote(null);
        workflow.setOverBudgetAmount(overBudgetAmount);
        workflow.setCreatedAt(Instant.now());
        workflow.setUpdatedAt(Instant.now());
        workflow.computeReadyForFinance();

        return workflowRepository.save(workflow);
    }

    /**
     * Update workflow when manager approves an expense.
     */
    public void updateManagerApproval(Long expenseId, Long managerId, String note) {
        workflowRepository.findByExpenseId(expenseId).ifPresent(workflow -> {
            workflow.setManagerApproved(true);
            workflow.setManagerApproverId(managerId);
            workflow.setManagerApprovedAt(Instant.now());
            workflow.setManagerNote(note);
            workflow.computeReadyForFinance();
            workflowRepository.save(workflow);
        });
    }

    /**
     * Clear workflow when expense is rejected (by manager or finance).
     */
    public void clearWorkflowOnRejection(Long expenseId) {
        workflowRepository.findByExpenseId(expenseId).ifPresent(workflow -> {
            workflow.setNeedsManagerApproval(false);
            workflow.setManagerApproved(false);
            workflow.setReadyForFinance(false);
            workflowRepository.save(workflow);
        });
    }

    /**
     * Get workflow state for an expense.
     */
    public Optional<ExpenseApprovalWorkflow> getWorkflowForExpense(Long expenseId) {
        return workflowRepository.findByExpenseId(expenseId);
    }

    /**
     * Check if expense is ready for finance review.
     */
    public boolean isReadyForFinance(Long expenseId) {
        return workflowRepository.findByExpenseId(expenseId)
            .map(ExpenseApprovalWorkflow::getReadyForFinance)
            .orElse(true); // Default to true if no workflow record (backward compatibility)
    }

    /**
     * Check if expense needs manager approval.
     */
    public boolean needsManagerApproval(Long expenseId) {
        return workflowRepository.findByExpenseId(expenseId)
            .map(ExpenseApprovalWorkflow::getNeedsManagerApproval)
            .orElse(false); // Default to false if no workflow record
    }
}