package com.expensehub.webbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.ApprovalRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private MobileExpenseClient mobileExpenseClient;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private DepartmentalBudgetService departmentalBudgetService;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(mobileExpenseClient, approvalRepository, departmentalBudgetService);
    }

    private MobileExpenseDTO expense(Long id, Long userId, String status, String amount, String category, String submittedAt) {
        MobileExpenseDTO e = new MobileExpenseDTO();
        e.setId(id);
        e.setUserId(userId);
        e.setStatus(status);
        e.setAmount(new BigDecimal(amount));
        e.setCategory(category);
        e.setSubmittedAt(LocalDateTime.parse(submittedAt));
        return e;
    }

    private MobileTripDTO trip(Long id, Long userId) {
        MobileTripDTO t = new MobileTripDTO();
        t.setId(id);
        t.setUserId(userId);
        return t;
    }

    private MobileUserDTO user(String username, String department) {
        MobileUserDTO u = new MobileUserDTO();
        u.setUsername(username);
        u.setDepartment(department);
        return u;
    }

    @Test
    void departmentExpenseComparison_onlyApproved_groupedByDepartment() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", "2026-08-01T10:00:00"),
                expense(2L, 10L, "APPROVED", "200", "HOTEL", "2026-08-02T10:00:00"),
                expense(3L, 10L, "REJECTED", "999", "MEAL", "2026-08-03T10:00:00"),
                expense(4L, 20L, "APPROVED", "50", "MEAL", "2026-08-04T10:00:00")));
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("alice", "Sales"));
        when(mobileExpenseClient.getUser(20L)).thenReturn(user("bob", "Marketing"));
        when(departmentalBudgetService.getLimit("Sales")).thenReturn(Optional.of(new BigDecimal("500")));
        when(departmentalBudgetService.getLimit("Marketing")).thenReturn(Optional.empty());

        List<Map<String, Object>> result = service.getDepartmentExpenseComparison();

        Map<String, Object> sales = result.stream().filter(r -> r.get("department").equals("Sales")).findFirst().orElseThrow();
        assertThat((BigDecimal) sales.get("totalExpense")).isEqualByComparingTo("300");
        assertThat((BigDecimal) sales.get("budget")).isEqualByComparingTo("500");

        Map<String, Object> marketing = result.stream().filter(r -> r.get("department").equals("Marketing")).findFirst().orElseThrow();
        assertThat((BigDecimal) marketing.get("totalExpense")).isEqualByComparingTo("50");
        assertThat((BigDecimal) marketing.get("budget")).isEqualByComparingTo("0");
    }

    @Test
    void employeeTravelFrequency_countsAndSortsDescending() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(
                trip(1L, 100L), trip(2L, 100L), trip(3L, 100L), trip(4L, 200L)));
        when(mobileExpenseClient.getUser(100L)).thenReturn(user("alice", "Sales"));
        when(mobileExpenseClient.getUser(200L)).thenReturn(user("bob", "Engineering"));

        List<Map<String, Object>> result = service.getEmployeeTravelFrequency();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("userName")).isEqualTo("alice");
        assertThat(result.get(0).get("tripCount")).isEqualTo(3L);
        assertThat(result.get(1).get("tripCount")).isEqualTo(1L);
    }

    @Test
    void budgetOverrunAlerts_flagsNearAndOverThresholds_excludesUnconfigured() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "4300", "FLIGHT", "2026-08-01T10:00:00"), // Sales: 4300/5000 = 86% -> NEAR
                expense(2L, 20L, "APPROVED", "6000", "FLIGHT", "2026-08-01T10:00:00"), // Marketing: 6000/3000 -> OVER
                expense(3L, 30L, "APPROVED", "9999", "FLIGHT", "2026-08-01T10:00:00"))); // R&D: no budget -> excluded
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("a", "Sales"));
        when(mobileExpenseClient.getUser(20L)).thenReturn(user("b", "Marketing"));
        when(mobileExpenseClient.getUser(30L)).thenReturn(user("c", "R&D"));
        when(departmentalBudgetService.getLimit("Sales")).thenReturn(Optional.of(new BigDecimal("5000")));
        when(departmentalBudgetService.getLimit("Marketing")).thenReturn(Optional.of(new BigDecimal("3000")));
        when(departmentalBudgetService.getLimit("R&D")).thenReturn(Optional.empty());

        List<Map<String, Object>> alerts = service.getBudgetOverrunAlerts();

        assertThat(alerts).hasSize(2);
        Map<String, Object> sales = alerts.stream().filter(a -> a.get("department").equals("Sales")).findFirst().orElseThrow();
        assertThat(sales.get("level")).isEqualTo("NEAR");
        Map<String, Object> marketing = alerts.stream().filter(a -> a.get("department").equals("Marketing")).findFirst().orElseThrow();
        assertThat(marketing.get("level")).isEqualTo("OVER");
        assertThat(alerts.stream().noneMatch(a -> a.get("department").equals("R&D"))).isTrue();
    }

    @Test
    void expenseCategoryBreakdown_sumsApprovedByCategory() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", "2026-08-01T10:00:00"),
                expense(2L, 10L, "APPROVED", "50", "FLIGHT", "2026-08-02T10:00:00"),
                expense(3L, 10L, "APPROVED", "30", "MEAL", "2026-08-03T10:00:00"),
                expense(4L, 10L, "REJECTED", "999", "MEAL", "2026-08-04T10:00:00")));

        List<Map<String, Object>> result = service.getExpenseCategoryBreakdown();

        Map<String, Object> flight = result.stream().filter(r -> r.get("category").equals("FLIGHT")).findFirst().orElseThrow();
        assertThat((BigDecimal) flight.get("amount")).isEqualByComparingTo("150");
        Map<String, Object> meal = result.stream().filter(r -> r.get("category").equals("MEAL")).findFirst().orElseThrow();
        assertThat((BigDecimal) meal.get("amount")).isEqualByComparingTo("30");
    }

    @Test
    void monthlySpendTrend_sumsApprovedByMonth_chronologicalOrder() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", "2026-07-15T10:00:00"),
                expense(2L, 10L, "APPROVED", "200", "FLIGHT", "2026-08-01T10:00:00"),
                expense(3L, 10L, "APPROVED", "50", "FLIGHT", "2026-08-10T10:00:00")));

        List<Map<String, Object>> result = service.getMonthlySpendTrend();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("month")).isEqualTo("2026-07");
        assertThat((BigDecimal) result.get(0).get("amount")).isEqualByComparingTo("100");
        assertThat(result.get(1).get("month")).isEqualTo("2026-08");
        assertThat((BigDecimal) result.get(1).get("amount")).isEqualByComparingTo("250");
    }

    @Test
    void approvalOutcomeSummary_countsStatusesAndAvgTurnaround() {
        LocalDateTime submitted = LocalDateTime.parse("2026-08-01T09:00:00");
        when(approvalRepository.findAll()).thenReturn(List.of(
                Approval.builder().status(ApprovalStatus.APPROVED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(10)).build(),
                Approval.builder().status(ApprovalStatus.APPROVED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(20)).build(),
                Approval.builder().status(ApprovalStatus.REJECTED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(5)).build(),
                Approval.builder().status(ApprovalStatus.PENDING)
                        .submittedAt(submitted).build()));

        Map<String, Object> result = service.getApprovalOutcomeSummary();

        assertThat(result.get("approved")).isEqualTo(2L);
        assertThat(result.get("rejected")).isEqualTo(1L);
        assertThat(result.get("pending")).isEqualTo(1L);
        assertThat((Double) result.get("avgTurnaroundHours")).isEqualTo(11.7);
    }
}
