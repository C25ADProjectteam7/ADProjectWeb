package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for "what is this department's currently active
 * budget", shared by ManagerService (Item 20 — trip approval reference) and
 * AnalyticsService (Item 23 — department expense comparison), so both read
 * the exact same figure that finance staff configure via BudgetConfig
 * (Item 16) instead of drifting into two independent implementations.
 * <p>
 * Quarter takes priority over annual when both are configured for the same
 * department. Empty means no budget has been configured for either period —
 * callers should treat that as "no limit set", not a limit of zero.
 */
@Service
public class BudgetLookupService {

    private final BudgetConfigRepository budgetConfigRepository;

    public BudgetLookupService(BudgetConfigRepository budgetConfigRepository) {
        this.budgetConfigRepository = budgetConfigRepository;
    }

    public Optional<BigDecimal> resolveBudgetLimit(String department) {
        LocalDate today = LocalDate.now();

        // 1.priority: quarterly budget for this quarter
        String quarterLabel = BudgetPeriodResolver.quarterlyLabel(today);
        Optional<BudgetConfig> quarterly = budgetConfigRepository
                .findByDepartmentAndPeriodTypeAndPeriodLabel(department, BudgetPeriodType.QUARTERLY, quarterLabel);
        if (quarterly.isPresent()) {
            return quarterly.map(BudgetConfig::getAmount);
        }
        // 2.fallback: annual budget for this year
        String yearLabel = BudgetPeriodResolver.annualLabel(today);
        Optional<BudgetConfig> annual = budgetConfigRepository
                .findByDepartmentAndPeriodTypeAndPeriodLabel(department, BudgetPeriodType.ANNUAL, yearLabel);
        if (annual.isPresent()) {
            return annual.map(BudgetConfig::getAmount);
        }
        // 3.fallback: most recent budget for this department, regardless of period type
        return budgetConfigRepository
                .findTopByDepartmentOrderByPeriodLabelDesc(department)
                .map(BudgetConfig::getAmount);
    }
}
