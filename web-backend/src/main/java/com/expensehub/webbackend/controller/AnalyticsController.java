package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.service.AnalyticsService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Corresponds to backlog Items 23-25: Data Visualization. */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/department-expenses")
    public List<Map<String, Object>> departmentExpenses(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getDepartmentExpenseComparison(period);
    }

    @GetMapping("/travel-frequency")
    public List<Map<String, Object>> travelFrequency(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getEmployeeTravelFrequency(period);
    }

    @GetMapping("/budget-alerts")
    public List<Map<String, Object>> budgetAlerts(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getBudgetOverrunAlerts(period);
    }

    @GetMapping("/budget-alerts/{department}/transactions")
    public List<Map<String, Object>> alertTransactions(@PathVariable String department) {
        return analyticsService.getAlertTransactions(department);
    }

    @GetMapping("/expense-categories")
    public List<Map<String, Object>> expenseCategories(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getExpenseCategoryBreakdown(period);
    }

    @GetMapping("/monthly-trend")
    public List<Map<String, Object>> monthlyTrend(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getMonthlySpendTrend(period);
    }

    @GetMapping("/approval-outcomes")
    public Map<String, Object> approvalOutcomes(
            @RequestParam(defaultValue = "this_month") String period) {
        return analyticsService.getApprovalOutcomeSummary(period);
    }
}
