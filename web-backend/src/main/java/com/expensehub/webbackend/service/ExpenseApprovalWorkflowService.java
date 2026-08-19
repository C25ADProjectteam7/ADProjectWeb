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
     *
     * @param departmentPeriodSpend total non-rejected spend for this expense's
     *     department over the budget period the expense falls in, INCLUDING this
     *     expense. Callers compute it from the full expense list (see
     *     FinanceServiceImpl.departmentPeriodSpend). Pass {@code null} only when
     *     the full list is genuinely unavailable — the expense's own amount is
     *     then used, which under-reports and can miss an over-budget condition.
     */
    public ExpenseApprovalWorkflow initializeWorkflowForExpense(
        MobileExpenseDTO expense,
        MobileUserDTO user,
        BigDecimal departmentPeriodSpend
    ) {
        BudgetEvaluation evaluation = evaluate(expense, user, departmentPeriodSpend);

        return createNewWorkflow(
            expense, user, evaluation.needsManagerApproval(), evaluation.overBudgetAmount());
    }

    /**
     * Update existing workflow (e.g., when budgets change).
     *
     * @param departmentPeriodSpend see
     *     {@link #initializeWorkflowForExpense(MobileExpenseDTO, MobileUserDTO, BigDecimal)}.
     */
    public ExpenseApprovalWorkflow updateWorkflowForExpense(
        Long expenseId,
        MobileExpenseDTO expense,
        MobileUserDTO user,
        BigDecimal departmentPeriodSpend
    ) {
        BudgetEvaluation evaluation = evaluate(expense, user, departmentPeriodSpend);
        boolean needsManagerApproval = evaluation.needsManagerApproval();
        BigDecimal overBudgetAmount = evaluation.overBudgetAmount();

        return workflowRepository.findByExpenseId(expenseId)
            .map(existing -> updateExistingWorkflow(existing, needsManagerApproval, overBudgetAmount))
            .orElseGet(() -> createNewWorkflow(expense, user, needsManagerApproval, overBudgetAmount));
    }

    /**
     * Single evaluation pass shared by initialize and update.
     * <p>
     * Both outputs are derived from the SAME (budget, periodSpent) pair, so
     * "does this need manager approval" and "by how much is it over" can no
     * longer disagree — and because the OVER_BUDGET decision is delegated to
     * {@link ReimbursementPolicyEngine}, it is also the same rule the finance
     * screen shows as a policy flag. Previously this class compared a single
     * expense against the whole budget while FinanceServiceImpl summed the
     * period, so an expense could be flagged OVER_BUDGET for finance yet never
     * routed to a manager.
     */
    private BudgetEvaluation evaluate(
        MobileExpenseDTO expense,
        MobileUserDTO user,
        BigDecimal departmentPeriodSpend
    ) {
        String department = user != null ? user.getDepartment() : null;

        // No department means no budget to compare against. The policy engine
        // can only raise OVER_BUDGET when a budget is present, so there is
        // nothing to evaluate here — this expense is never routed to a manager.
        // (MISSING_RECEIPT and OVER_PER_DIEM still surface to finance via
        // FinanceServiceImpl.computeFlags; they don't gate manager approval.)
        if (department == null) {
            return new BudgetEvaluation(false, null);
        }

        boolean receiptAttached =
            expense.getReceiptUrl() != null && !expense.getReceiptUrl().isBlank();
        ReimbursementCategory category = ReimbursementCategory.valueOf(expense.getCategory());

        LocalDate anchor = expense.getSubmittedAt() != null
            ? expense.getSubmittedAt().toLocalDate()
            : LocalDate.now();

        Optional<BudgetConfig> budget = findBudgetForDepartmentAndDate(department, anchor);

        // Fall back to the expense's own amount only if the caller could not
        // supply the period total. That under-reports spend, so it can miss an
        // over-budget condition, but it never invents one.
        BigDecimal periodSpent =
            departmentPeriodSpend != null ? departmentPeriodSpend : expense.getAmount();

        List<String> flags =
            policyEngine.evaluate(
                expense.getAmount(), category, receiptAttached, budget, periodSpent);

        boolean overBudget = flags.contains(ReimbursementPolicyEngine.FLAG_OVER_BUDGET);
        if (!overBudget) {
            return new BudgetEvaluation(false, null);
        }

        // How much of THIS expense falls beyond the budget line — not how far
        // the department as a whole is over.
        //
        // periodSpent - budget is the department's cumulative overshoot. Using
        // it directly means that once a department is over, every subsequent
        // expense row shows the same (and growing) figure, so two S$300 claims
        // can both read "over by S$200" when the department is only S$200 over
        // in total. Capping at the expense's own amount gives the manager the
        // number they're actually deciding on:
        //   - department was still under: overshoot = thisAmount - remaining
        //   - department already over:    the whole expense is beyond the line
        BigDecimal cumulativeOvershoot = periodSpent.subtract(budget.orElseThrow().getAmount());
        BigDecimal overBudgetAmount = cumulativeOvershoot.min(expense.getAmount());

        return new BudgetEvaluation(true, overBudgetAmount);
    }

    /** Result of one budget evaluation: whether a manager is needed, and by how much. */
    private record BudgetEvaluation(boolean needsManagerApproval, BigDecimal overBudgetAmount) {}

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
        // Once a manager has made a decision (approved or rejected), budget
        // recalculation must not reverse it. The manager acted on the state at
        // the time of review; silently flipping readyForFinance back to false
        // would block Finance Staff from acting on an already-approved expense.
        // The over-budget amount can still be updated for informational display.
        if (existing.getManagerApproved() != null) {
            if (!sameAmount(overBudgetAmount, existing.getOverBudgetAmount())) {
                existing.setOverBudgetAmount(overBudgetAmount);
                return workflowRepository.save(existing);
            }
            return existing;
        }

        boolean changed = false;

        if (!Boolean.valueOf(needsManagerApproval).equals(existing.getNeedsManagerApproval())) {
            existing.setNeedsManagerApproval(needsManagerApproval);
            changed = true;
        }

        if (!sameAmount(overBudgetAmount, existing.getOverBudgetAmount())) {
            existing.setOverBudgetAmount(overBudgetAmount);
            changed = true;
        }

        if (!changed) {
            return existing;
        }

        existing.computeReadyForFinance();
        return workflowRepository.save(existing);
    }

    /**
     * BigDecimal.equals() is scale-sensitive — 10.0 and 10.00 compare unequal
     * and would trigger a write on every read. compareTo() is the correct test.
     */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
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