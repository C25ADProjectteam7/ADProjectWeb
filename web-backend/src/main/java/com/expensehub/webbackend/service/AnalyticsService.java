package com.expensehub.webbackend.service;

import com.expensehub.webbackend.client.MobileApiClient;
import com.expensehub.webbackend.dto.MobileExpenseDto;
import com.expensehub.webbackend.dto.MobileTripDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Corresponds to backlog Items 23-25: Data Visualization.
 * Web and Mobile are separate databases, so a SQL GROUP BY across them isn't
 * possible. Raw rows are fetched from Mobile over HTTP and aggregated here in
 * Java instead.
 */
@Service
public class AnalyticsService {

    private final MobileApiClient mobileApiClient;

    // TODO: replace with a real lookup once Item 16 (Department Budget
    // Configuration) exists.
    private static final Map<String, BigDecimal> DEPARTMENT_BUDGETS = Map.of(
            "Sales", BigDecimal.valueOf(3500),
            "Engineering", BigDecimal.valueOf(4000),
            "Marketing", BigDecimal.valueOf(3000));

    public AnalyticsService(MobileApiClient mobileApiClient) {
        this.mobileApiClient = mobileApiClient;
    }

    public List<Map<String, Object>> getDepartmentExpenseComparison() {
        List<MobileExpenseDto> expenses = mobileApiClient.fetchAllExpenses();

        Map<String, BigDecimal> totalsByDept = expenses.stream()
                .collect(Collectors.groupingBy(
                        MobileExpenseDto::department,
                        Collectors.reducing(BigDecimal.ZERO, MobileExpenseDto::amount, BigDecimal::add)));

        return totalsByDept.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "department", entry.getKey(),
                        "totalExpense", entry.getValue(),
                        "budget", DEPARTMENT_BUDGETS.getOrDefault(entry.getKey(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getEmployeeTravelFrequency() {
        List<MobileTripDto> trips = mobileApiClient.fetchAllTrips();

        Map<Long, Long> tripCountByUser = trips.stream()
                .collect(Collectors.groupingBy(MobileTripDto::userId, Collectors.counting()));

        Map<Long, MobileTripDto> sampleByUser = trips.stream()
                .collect(Collectors.toMap(MobileTripDto::userId, t -> t, (a, b) -> a));

        return tripCountByUser.entrySet().stream()
                .map(entry -> {
                    MobileTripDto sample = sampleByUser.get(entry.getKey());
                    return Map.<String, Object>of(
                            "userId", entry.getKey(),
                            "userName", sample.employeeName(),
                            "department", sample.department(),
                            "tripCount", entry.getValue());
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBudgetOverrunAlerts() {
        return getDepartmentExpenseComparison().stream()
                .filter(row -> {
                    BigDecimal actual = (BigDecimal) row.get("totalExpense");
                    BigDecimal budget = (BigDecimal) row.get("budget");
                    return budget.compareTo(BigDecimal.ZERO) > 0 && actual.compareTo(budget) > 0;
                })
                .map(row -> {
                    BigDecimal actual = (BigDecimal) row.get("totalExpense");
                    BigDecimal budget = (BigDecimal) row.get("budget");
                    int overPercent = actual.subtract(budget)
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
}