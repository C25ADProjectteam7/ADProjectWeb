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
    public List<Map<String, Object>> departmentExpenses() {
        return analyticsService.getDepartmentExpenseComparison();
    }

    @GetMapping("/travel-frequency")
    public List<Map<String, Object>> travelFrequency() {
        return analyticsService.getEmployeeTravelFrequency();
    }

    @GetMapping("/budget-alerts")
    public List<Map<String, Object>> budgetAlerts() {
        return analyticsService.getBudgetOverrunAlerts();
    }
}