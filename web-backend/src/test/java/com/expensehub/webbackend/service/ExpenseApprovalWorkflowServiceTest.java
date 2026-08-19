package com.expensehub.webbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import com.expensehub.webbackend.repository.ExpenseApprovalWorkflowRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the manager-routing rule against the regression it was fixed for:
 * this service used to compare a single expense against the whole department
 * budget, while FinanceServiceImpl summed the period. An expense could then be
 * shown as OVER_BUDGET on the finance screen yet never reach a manager.
 */
class ExpenseApprovalWorkflowServiceTest {

    private ExpenseApprovalWorkflowRepository workflowRepository;
    private BudgetConfigRepository budgetConfigRepository;
    private ExpenseApprovalWorkflowService service;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(ExpenseApprovalWorkflowRepository.class);
        budgetConfigRepository = mock(BudgetConfigRepository.class);
        service =
                new ExpenseApprovalWorkflowService(
                        workflowRepository, budgetConfigRepository, new ReimbursementPolicyEngine());

        when(workflowRepository.save(any(ExpenseApprovalWorkflow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MobileExpenseDTO expense(String amount) {
        MobileExpenseDTO dto = new MobileExpenseDTO();
        dto.setId(1L);
        dto.setTripId(1L);
        dto.setUserId(1L);
        dto.setCategory("MEAL");
        dto.setAmount(new BigDecimal(amount));
        dto.setCurrency("SGD");
        dto.setReceiptUrl("/uploads/receipts/2026-02-10/a.jpg");
        dto.setStatus("SUBMITTED");
        dto.setSubmittedAt(LocalDateTime.of(2026, 2, 10, 9, 0));
        return dto;
    }

    private MobileUserDTO user(String department) {
        MobileUserDTO dto = new MobileUserDTO();
        dto.setId(1L);
        dto.setUsername("alice");
        dto.setDepartment(department);
        dto.setRole("EMPLOYEE");
        return dto;
    }

    private void budgetOf(String amount) {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                        "Engineering", BudgetPeriodType.QUARTERLY, "2026-Q1"))
                .thenReturn(
                        Optional.of(
                                BudgetConfig.builder()
                                        .id(1L)
                                        .department("Engineering")
                                        .periodType(BudgetPeriodType.QUARTERLY)
                                        .periodLabel("2026-Q1")
                                        .amount(new BigDecimal(amount))
                                        .build()));
    }

    @Test
    void smallExpenseNeedsManagerApprovalOncePeriodSpendExceedsBudget() {
        budgetOf("1000.00");
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        // The expense itself is only 40, so the old single-expense comparison
        // would have said "under budget". The department has already spent
        // 1200 this quarter against a 1000 budget, so it must go to a manager —
        // and all 40 of this claim sits beyond the budget line.
        ExpenseApprovalWorkflow workflow =
                service.updateWorkflowForExpense(
                        1L, expense("40.00"), user("Engineering"), new BigDecimal("1200.00"));

        assertThat(workflow.getNeedsManagerApproval()).isTrue();
        assertThat(workflow.getOverBudgetAmount()).isEqualByComparingTo("40.00");
        assertThat(workflow.getReadyForFinance()).isFalse();
    }

    @Test
    void largeExpenseWithinPeriodBudgetDoesNotNeedManagerApproval() {
        budgetOf("5000.00");
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        ExpenseApprovalWorkflow workflow =
                service.updateWorkflowForExpense(
                        1L, expense("900.00"), user("Engineering"), new BigDecimal("900.00"));

        assertThat(workflow.getNeedsManagerApproval()).isFalse();
        assertThat(workflow.getOverBudgetAmount()).isNull();
        assertThat(workflow.getReadyForFinance()).isTrue();
    }

    @Test
    void onlyThePortionBeyondTheRemainingBudgetIsReported() {
        budgetOf("1000.00");
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        // Department had spent 900 of its 1000 budget, so 100 remained. This
        // 300 claim crosses the line: 200 of it is beyond, 100 still fits.
        ExpenseApprovalWorkflow workflow =
                service.updateWorkflowForExpense(
                        1L, expense("300.00"), user("Engineering"), new BigDecimal("1200.00"));

        assertThat(workflow.getNeedsManagerApproval()).isTrue();
        assertThat(workflow.getOverBudgetAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void overBudgetAmountIsCappedAtThisExpensesOwnAmount() {
        budgetOf("1000.00");
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        // Department was already 500 over before this 40 claim arrived. The
        // cumulative overshoot is 540, but only 40 of it belongs to this
        // expense — reporting 540 on this row would double-count across every
        // pending claim in the department.
        ExpenseApprovalWorkflow workflow =
                service.updateWorkflowForExpense(
                        1L, expense("40.00"), user("Engineering"), new BigDecimal("1540.00"));

        assertThat(workflow.getNeedsManagerApproval()).isTrue();
        assertThat(workflow.getOverBudgetAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void overBudgetAmountMatchesThePolicyEngineThreshold() {
        budgetOf("1000.00");
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        // Exactly at the budget is NOT over — the policy engine uses a strict
        // greater-than. This pins the boundary so the two stay in step.
        ExpenseApprovalWorkflow atLimit =
                service.updateWorkflowForExpense(
                        1L, expense("40.00"), user("Engineering"), new BigDecimal("1000.00"));

        assertThat(atLimit.getNeedsManagerApproval()).isFalse();
        assertThat(atLimit.getOverBudgetAmount()).isNull();
    }

    @Test
    void expenseWithoutDepartmentIsNeverRoutedToAManager() {
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.empty());

        ExpenseApprovalWorkflow workflow =
                service.updateWorkflowForExpense(1L, expense("9999.00"), null, null);

        assertThat(workflow.getNeedsManagerApproval()).isFalse();
        assertThat(workflow.getDepartment()).isEqualTo("Unknown");
        assertThat(workflow.getReadyForFinance()).isTrue();
    }

    @Test
    void staleOverBudgetAmountIsClearedWhenBudgetIsRaised() {
        budgetOf("5000.00");
        ExpenseApprovalWorkflow existing = new ExpenseApprovalWorkflow();
        existing.setExpenseId(1L);
        existing.setDepartment("Engineering");
        existing.setNeedsManagerApproval(false);
        existing.setOverBudgetAmount(new BigDecimal("200.00"));
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.of(existing));

        // needsManagerApproval is unchanged (false), but the over-budget figure
        // is now stale. The old code returned early and left 200.00 on the row.
        ExpenseApprovalWorkflow updated =
                service.updateWorkflowForExpense(
                        1L, expense("40.00"), user("Engineering"), new BigDecimal("900.00"));

        assertThat(updated.getOverBudgetAmount()).isNull();
        verify(workflowRepository).save(existing);
    }

    @Test
    void unchangedWorkflowIsNotRewrittenOnEveryRead() {
        budgetOf("1000.00");
        ExpenseApprovalWorkflow existing = new ExpenseApprovalWorkflow();
        existing.setExpenseId(1L);
        existing.setDepartment("Engineering");
        existing.setNeedsManagerApproval(true);
        // The computed figure will be 40.00; MySQL hands back 40.0 here.
        // Same value, different scale — BigDecimal.equals() calls these unequal
        // and would trigger a pointless write on every single list request.
        existing.setOverBudgetAmount(new BigDecimal("40.0"));
        when(workflowRepository.findByExpenseId(1L)).thenReturn(Optional.of(existing));

        service.updateWorkflowForExpense(
                1L, expense("40.00"), user("Engineering"), new BigDecimal("1200.00"));

        verify(workflowRepository, never()).save(any(ExpenseApprovalWorkflow.class));
    }
}
