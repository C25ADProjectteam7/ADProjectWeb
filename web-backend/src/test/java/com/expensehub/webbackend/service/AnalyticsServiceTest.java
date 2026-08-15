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
import java.time.LocalDate;
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
    @Mock private BudgetLookupService budgetLookupService;

    private AnalyticsService service;

    // All test dates are computed relative to "today" rather than hardcoded,
    // so these tests stay valid regardless of when they're run.
    private static final LocalDate TODAY = LocalDate.now();

    private LocalDateTime inThisMonth(int dayOffset) {
        return TODAY.withDayOfMonth(1).plusDays(dayOffset).atTime(10, 0);
    }

    private LocalDateTime inLastMonth(int dayOffset) {
        return TODAY.withDayOfMonth(1).minusMonths(1).plusDays(dayOffset).atTime(10, 0);
    }

    private LocalDateTime monthsAgo(int months, int dayOffset) {
        return TODAY.withDayOfMonth(1).minusMonths(months).plusDays(dayOffset).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(mobileExpenseClient, approvalRepository, budgetLookupService);
    }

    private MobileExpenseDTO expense(Long id, Long userId, String status, String amount, String category, LocalDateTime submittedAt) {
        MobileExpenseDTO e = new MobileExpenseDTO();
        e.setId(id);
        e.setUserId(userId);
        e.setStatus(status);
        e.setAmount(new BigDecimal(amount));
        e.setCategory(category);
        e.setSubmittedAt(submittedAt);
        return e;
    }

    private MobileTripDTO trip(Long id, Long userId, LocalDateTime createdAt) {
        MobileTripDTO t = new MobileTripDTO();
        t.setId(id);
        t.setUserId(userId);
        t.setCreatedAt(createdAt.toString());
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
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", inThisMonth(1)),
                expense(2L, 10L, "APPROVED", "200", "HOTEL", inThisMonth(2)),
                expense(3L, 10L, "REJECTED", "999", "MEAL", inThisMonth(3)),
                expense(4L, 20L, "APPROVED", "50", "MEAL", inThisMonth(4))));
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("alice", "Sales"));
        when(mobileExpenseClient.getUser(20L)).thenReturn(user("bob", "Marketing"));
        when(budgetLookupService.resolveBudgetLimit("Sales")).thenReturn(Optional.of(new BigDecimal("500")));
        when(budgetLookupService.resolveBudgetLimit("Marketing")).thenReturn(Optional.empty());

        List<Map<String, Object>> result = service.getDepartmentExpenseComparison("this_month");

        Map<String, Object> sales = result.stream().filter(r -> r.get("department").equals("Sales")).findFirst().orElseThrow();
        assertThat((BigDecimal) sales.get("totalExpense")).isEqualByComparingTo("300");
        assertThat((BigDecimal) sales.get("budget")).isEqualByComparingTo("500");

        Map<String, Object> marketing = result.stream().filter(r -> r.get("department").equals("Marketing")).findFirst().orElseThrow();
        assertThat((BigDecimal) marketing.get("totalExpense")).isEqualByComparingTo("50");
        assertThat((BigDecimal) marketing.get("budget")).isEqualByComparingTo("0");
    }

    @Test
    void departmentExpenseComparison_lastMonthPeriod_excludesThisMonthData() {
        // This is the exact scenario that surfaced the production bug:
        // an expense submitted this month must NOT appear when filtering
        // for "last_month".
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "55", "HOTEL", inThisMonth(1))));

        List<Map<String, Object>> result = service.getDepartmentExpenseComparison("last_month");

        assertThat(result).isEmpty();
    }

    @Test
    void departmentExpenseComparison_lastMonthPeriod_includesLastMonthData() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "55", "HOTEL", inLastMonth(1))));
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("alice", "Sales"));
        when(budgetLookupService.resolveBudgetLimit("Sales")).thenReturn(Optional.empty());

        List<Map<String, Object>> result = service.getDepartmentExpenseComparison("last_month");

        assertThat(result).hasSize(1);
        assertThat((BigDecimal) result.get(0).get("totalExpense")).isEqualByComparingTo("55");
    }

    @Test
    void departmentExpenseComparison_thisQuarterPeriod_excludesDataFromFourMonthsAgo() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "55", "HOTEL", monthsAgo(4, 1))));

        List<Map<String, Object>> result = service.getDepartmentExpenseComparison("this_quarter");

        assertThat(result).isEmpty();
    }

    @Test
    void employeeTravelFrequency_countsAndSortsDescending() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(
                trip(1L, 100L, inThisMonth(1)), trip(2L, 100L, inThisMonth(2)),
                trip(3L, 100L, inThisMonth(3)), trip(4L, 200L, inThisMonth(4))));
        when(mobileExpenseClient.getUser(100L)).thenReturn(user("alice", "Sales"));
        when(mobileExpenseClient.getUser(200L)).thenReturn(user("bob", "Engineering"));

        List<Map<String, Object>> result = service.getEmployeeTravelFrequency("this_month");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("userName")).isEqualTo("alice");
        assertThat(result.get(0).get("tripCount")).isEqualTo(3L);
        assertThat(result.get(1).get("tripCount")).isEqualTo(1L);
    }

    @Test
    void budgetOverrunAlerts_flagsNearAndOverThresholds_excludesUnconfigured() {
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "4300", "FLIGHT", inThisMonth(1)), // Sales: 4300/5000 = 86% -> NEAR
                expense(2L, 20L, "APPROVED", "6000", "FLIGHT", inThisMonth(1)), // Marketing: 6000/3000 -> OVER
                expense(3L, 30L, "APPROVED", "9999", "FLIGHT", inThisMonth(1)))); // R&D: no budget -> excluded
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("a", "Sales"));
        when(mobileExpenseClient.getUser(20L)).thenReturn(user("b", "Marketing"));
        when(mobileExpenseClient.getUser(30L)).thenReturn(user("c", "R&D"));
        when(budgetLookupService.resolveBudgetLimit("Sales")).thenReturn(Optional.of(new BigDecimal("5000")));
        when(budgetLookupService.resolveBudgetLimit("Marketing")).thenReturn(Optional.of(new BigDecimal("3000")));
        when(budgetLookupService.resolveBudgetLimit("R&D")).thenReturn(Optional.empty());

        List<Map<String, Object>> alerts = service.getBudgetOverrunAlerts("this_month");

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
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", inThisMonth(1)),
                expense(2L, 10L, "APPROVED", "50", "FLIGHT", inThisMonth(2)),
                expense(3L, 10L, "APPROVED", "30", "MEAL", inThisMonth(3)),
                expense(4L, 10L, "REJECTED", "999", "MEAL", inThisMonth(4))));

        List<Map<String, Object>> result = service.getExpenseCategoryBreakdown("this_month");

        Map<String, Object> flight = result.stream().filter(r -> r.get("category").equals("FLIGHT")).findFirst().orElseThrow();
        assertThat((BigDecimal) flight.get("amount")).isEqualByComparingTo("150");
        Map<String, Object> meal = result.stream().filter(r -> r.get("category").equals("MEAL")).findFirst().orElseThrow();
        assertThat((BigDecimal) meal.get("amount")).isEqualByComparingTo("30");
    }

    @Test
    void monthlySpendTrend_sumsApprovedByMonth_chronologicalOrder() {
        // Uses "this_quarter" (not "this_month") because a trend chart's whole
        // purpose is showing multiple months — scoping it to "this_month" would
        // only ever show a single bucket. Data spans this month and the quarter's
        // first month, which are guaranteed distinct for most of the year (they
        // only coincide when today falls in a quarter's first month).
        LocalDateTime earlier = TODAY.withDayOfMonth(1).getMonthValue() == (((TODAY.getMonthValue() - 1) / 3) * 3 + 1)
                ? inThisMonth(5)
                : LocalDate.of(TODAY.getYear(), ((TODAY.getMonthValue() - 1) / 3) * 3 + 1, 1).plusDays(1).atTime(10, 0);
        when(mobileExpenseClient.listAllExpenses()).thenReturn(List.of(
                expense(1L, 10L, "APPROVED", "100", "FLIGHT", earlier),
                expense(2L, 10L, "APPROVED", "200", "FLIGHT", inThisMonth(1)),
                expense(3L, 10L, "APPROVED", "50", "FLIGHT", inThisMonth(2))));

        List<Map<String, Object>> result = service.getMonthlySpendTrend("this_quarter");

        BigDecimal total = result.stream()
                .map(r -> (BigDecimal) r.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("350");
        assertThat(result).allSatisfy(r -> assertThat((String) r.get("month")).startsWith("20"));
    }

    @Test
    void approvalOutcomeSummary_countsStatusesAndAvgTurnaround() {
        LocalDateTime submitted = inThisMonth(1);
        when(approvalRepository.findAll()).thenReturn(List.of(
                Approval.builder().status(ApprovalStatus.APPROVED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(10)).build(),
                Approval.builder().status(ApprovalStatus.APPROVED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(20)).build(),
                Approval.builder().status(ApprovalStatus.REJECTED)
                        .submittedAt(submitted).decidedAt(submitted.plusHours(5)).build(),
                Approval.builder().status(ApprovalStatus.PENDING)
                        .submittedAt(submitted).build()));

        Map<String, Object> result = service.getApprovalOutcomeSummary("this_month");

        assertThat(result.get("approved")).isEqualTo(2L);
        assertThat(result.get("rejected")).isEqualTo(1L);
        assertThat(result.get("pending")).isEqualTo(1L);
        assertThat((Double) result.get("avgTurnaroundHours")).isEqualTo(11.7);
    }
}
