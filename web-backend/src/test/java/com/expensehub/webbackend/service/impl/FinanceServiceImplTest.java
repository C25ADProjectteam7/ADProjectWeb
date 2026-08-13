package com.expensehub.webbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.dto.BudgetConfigRequest;
import com.expensehub.webbackend.dto.BudgetConfigResponse;
import com.expensehub.webbackend.dto.ReimbursementResponse;
import com.expensehub.webbackend.dto.ReimbursementReviewRequest;
import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.entity.ReimbursementAuditLog;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.BudgetAuditLogRepository;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import com.expensehub.webbackend.repository.ReimbursementAuditLogRepository;
import com.expensehub.webbackend.service.ReimbursementPolicyEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;


class FinanceServiceImplTest {

    private BudgetConfigRepository budgetConfigRepository;
    private BudgetAuditLogRepository budgetAuditLogRepository;
    private ReimbursementAuditLogRepository reimbursementAuditLogRepository;
    private MobileExpenseClient mobileExpenseClient;
    private FinanceServiceImpl financeService;

    @BeforeEach
    void setUp() {
        budgetConfigRepository = mock(BudgetConfigRepository.class);
        budgetAuditLogRepository = mock(BudgetAuditLogRepository.class);
        reimbursementAuditLogRepository = mock(ReimbursementAuditLogRepository.class);
        mobileExpenseClient = mock(MobileExpenseClient.class);

        financeService =
                new FinanceServiceImpl(
                        budgetConfigRepository,
                        budgetAuditLogRepository,
                        reimbursementAuditLogRepository,
                        new ReimbursementPolicyEngine(),
                        new ReimbursementExcelExporter(),
                        mobileExpenseClient);

        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                        any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private MobileExpenseDTO expense(
            long id, long userId, String category, String amount, String status, boolean hasReceipt) {
        MobileExpenseDTO dto = new MobileExpenseDTO();
        dto.setId(id);
        dto.setTripId(1L);
        dto.setUserId(userId);
        dto.setCategory(category);
        dto.setAmount(new BigDecimal(amount));
        dto.setCurrency("CNY");
        dto.setDescription("test expense");
        dto.setReceiptUrl(hasReceipt ? "/uploads/receipts/2026-02-10/abc.jpg" : null);
        dto.setStatus(status);
        dto.setSubmittedAt(LocalDateTime.of(2026, 2, 10, 9, 0));
        dto.setCreatedAt(LocalDateTime.of(2026, 2, 10, 9, 0));
        return dto;
    }

    private MobileUserDTO user(long id, String username, String department) {
        MobileUserDTO dto = new MobileUserDTO();
        dto.setId(id);
        dto.setUsername(username);
        dto.setDepartment(department);
        dto.setRole("EMPLOYEE");
        return dto;
    }

    @Test
    void listReimbursementsResolvesDepartmentAndBuildsFullReceiptUrl() {
        when(mobileExpenseClient.listAllExpenses())
                .thenReturn(
                        List.of(
                                expense(101L, 1L, "MEAL", "42.50", "SUBMITTED", true),
                                expense(102L, 2L, "HOTEL", "620.00", "SUBMITTED", false)));
        when(mobileExpenseClient.getUser(1L)).thenReturn(user(1L, "alice", "Engineering"));
        when(mobileExpenseClient.getUser(2L)).thenReturn(user(2L, "ben", "Sales"));

        Page<ReimbursementResponse> page =
                financeService.listReimbursements(
                        null, "Engineering", null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        ReimbursementResponse r = page.getContent().get(0);
        assertThat(r.department()).isEqualTo("Engineering");
        assertThat(r.employeeName()).isEqualTo("alice");
        assertThat(r.receiptUrl()).isEqualTo("/mobile-uploads/receipts/2026-02-10/abc.jpg");
    }

    @Test
    void listReimbursementsFlagsMissingReceiptAndOverPerDiem() {
        when(mobileExpenseClient.listAllExpenses())
                .thenReturn(List.of(expense(201L, 1L, "MEAL", "500.00", "SUBMITTED", false)));
        when(mobileExpenseClient.getUser(1L)).thenReturn(user(1L, "alice", "Engineering"));

        Page<ReimbursementResponse> page =
                financeService.listReimbursements(null, null, null, null, null, PageRequest.of(0, 10));

        List<String> flags = page.getContent().get(0).policyFlags();
        assertThat(flags)
                .contains(ReimbursementPolicyEngine.FLAG_MISSING_RECEIPT, ReimbursementPolicyEngine.FLAG_OVER_PER_DIEM);
    }

    @Test
    void reviewApproveCallsMobileAndRecordsLocalAuditEntry() {
        MobileExpenseDTO approved = expense(301L, 1L, "TRANSPORT", "35.00", "APPROVED", true);
        approved.setApprovalOpinion("Looks good");
        approved.setApproverName("finance@expensehub.com");

        when(mobileExpenseClient.approve(301L, "Looks good")).thenReturn(approved);
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(approved));
        when(mobileExpenseClient.getUser(1L)).thenReturn(user(1L, "alice", "Engineering"));

        ReimbursementResponse response =
                financeService.reviewReimbursement(
                        301L,
                        new ReimbursementReviewRequest(
                                ReimbursementReviewRequest.ReviewDecision.APPROVE, "Looks good"),
                        "finance@expensehub.com");

        assertThat(response.status()).isEqualTo(ReimbursementStatus.APPROVED);
        assertThat(response.reviewedBy()).isEqualTo("finance@expensehub.com");
        verify(mobileExpenseClient).approve(301L, "Looks good");
        verify(reimbursementAuditLogRepository).save(any(ReimbursementAuditLog.class));
    }

    @Test
    void budgetSpentIsSumOfNonRejectedExpensesInDepartmentAndPeriod() {
        BudgetConfig config =
                BudgetConfig.builder()
                        .id(1L)
                        .department("Engineering")
                        .periodType(BudgetPeriodType.QUARTERLY)
                        .periodLabel("2026-Q1")
                        .amount(new BigDecimal("500.00"))
                        .build();
        when(budgetConfigRepository.findAllByOrderByDepartmentAscPeriodLabelAsc())
                .thenReturn(List.of(config));
        when(mobileExpenseClient.listAllExpenses())
                .thenReturn(
                        List.of(
                                expense(401L, 1L, "MEAL", "300.00", "APPROVED", true),
                                expense(402L, 1L, "TRANSPORT", "250.00", "REJECTED", true)));
        when(mobileExpenseClient.getUser(1L)).thenReturn(user(1L, "alice", "Engineering"));

        List<BudgetConfigResponse> budgets = financeService.listBudgets(null);

        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0).spent()).isEqualByComparingTo("300.00");
        assertThat(budgets.get(0).overBudget()).isFalse();
    }

    @Test
    void upsertBudgetSavesConfigAndAuditLog() {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                        "Engineering", BudgetPeriodType.QUARTERLY, "2026-Q1"))
                .thenReturn(Optional.empty());
        when(budgetConfigRepository.save(any(BudgetConfig.class)))
                .thenAnswer(
                        invocation -> {
                            BudgetConfig c = invocation.getArgument(0);
                            c.setId(9L);
                            return c;
                        });
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of());

        BudgetConfigResponse response =
                financeService.upsertBudget(
                        new BudgetConfigRequest(
                                "Engineering", BudgetPeriodType.QUARTERLY, "2026-Q1", new BigDecimal("5000.00")),
                        "finance@expensehub.com");

        assertThat(response.department()).isEqualTo("Engineering");
        assertThat(response.amount()).isEqualByComparingTo("5000.00");
        verify(budgetAuditLogRepository).save(any());
    }
}
