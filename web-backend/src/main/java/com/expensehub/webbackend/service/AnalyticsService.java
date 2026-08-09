package com.expensehub.webbackend.service;

import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Corresponds to backlog Items 23-25: Data Visualization.
 * Web and Mobile are separate databases, so a SQL GROUP BY across them isn't
 * possible. Raw rows are fetched from Mobile over HTTP and aggregated here in
 * Java instead. Expense/trip records only carry a userId, not a department
 * name, so department has to be resolved per user via GET /api/users/{id}
 * (cached per call) before grouping.
 */
@Service
public class AnalyticsService {

    private final MobileExpenseClient mobileExpenseClient;

    // TODO: replace with a real lookup against DepartmentalBudgetService/BudgetConfig
    // once it's clear which of the two budget models this dashboard should read from.
    private static final Map<String, BigDecimal> DEPARTMENT_BUDGETS =
            Map.of(
                    "Sales", BigDecimal.valueOf(3500),
                    "Engineering", BigDecimal.valueOf(4000),
                    "Marketing", BigDecimal.valueOf(3000));

    public AnalyticsService(MobileExpenseClient mobileExpenseClient) {
        this.mobileExpenseClient = mobileExpenseClient;
    }

    public List<Map<String, Object>> getDepartmentExpenseComparison() {
        List<MobileExpenseDTO> expenses = mobileExpenseClient.listAllExpenses();
        Map<Long, MobileUserDTO> userCache = new HashMap<>();

        Map<String, BigDecimal> totalsByDept =
                expenses.stream()
                        .collect(
                                Collectors.groupingBy(
                                        e -> resolveDepartment(userCache, e.getUserId()),
                                        Collectors.reducing(
                                                BigDecimal.ZERO, MobileExpenseDTO::getAmount, BigDecimal::add)));

        return totalsByDept.entrySet().stream()
                .map(
                        entry ->
                                Map.<String, Object>of(
                                        "department", entry.getKey(),
                                        "totalExpense", entry.getValue(),
                                        "budget", DEPARTMENT_BUDGETS.getOrDefault(entry.getKey(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getEmployeeTravelFrequency() {
        List<MobileTripDTO> trips = mobileExpenseClient.listAllTrips();
        Map<Long, MobileUserDTO> userCache = new HashMap<>();

        Map<Long, Long> tripCountByUser =
                trips.stream()
                        .collect(Collectors.groupingBy(MobileTripDTO::getUserId, Collectors.counting()));

        return tripCountByUser.entrySet().stream()
                .map(
                        entry -> {
                            MobileUserDTO user = resolveUser(userCache, entry.getKey());
                            return Map.<String, Object>of(
                                    "userId", entry.getKey(),
                                    "userName", user != null ? user.getUsername() : "Unknown",
                                    "department", user != null && user.getDepartment() != null
                                            ? user.getDepartment()
                                            : "Unknown",
                                    "tripCount", entry.getValue());
                        })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBudgetOverrunAlerts() {
        return getDepartmentExpenseComparison().stream()
                .filter(
                        row -> {
                            BigDecimal actual = (BigDecimal) row.get("totalExpense");
                            BigDecimal budget = (BigDecimal) row.get("budget");
                            return budget.compareTo(BigDecimal.ZERO) > 0 && actual.compareTo(budget) > 0;
                        })
                .map(
                        row -> {
                            BigDecimal actual = (BigDecimal) row.get("totalExpense");
                            BigDecimal budget = (BigDecimal) row.get("budget");
                            int overPercent =
                                    actual.subtract(budget)
                                            .multiply(BigDecimal.valueOf(100))
                                            .divide(budget, 0, java.math.RoundingMode.HALF_UP)
                                            .intValue();
                            return Map.<String, Object>of(
                                    "department", row.get("department"),
                                    "budget", budget,
                                    "actual", actual,
                                    "overPercent", overPercent);
                        })
                .collect(Collectors.toList());
    }

    private String resolveDepartment(Map<Long, MobileUserDTO> cache, Long userId) {
        MobileUserDTO user = resolveUser(cache, userId);
        return user != null && user.getDepartment() != null ? user.getDepartment() : "Unknown";
    }

    private MobileUserDTO resolveUser(Map<Long, MobileUserDTO> cache, Long userId) {
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        MobileUserDTO user = null;
        try {
            user = mobileExpenseClient.getUser(userId);
        } catch (Exception e) {
            // A single missing/unreachable user shouldn't take down the whole dashboard.
        }
        cache.put(userId, user);
        return user;
    }
}
