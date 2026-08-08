package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.BudgetPeriodType;
import java.math.BigDecimal;
import java.time.Instant;

/** Budget configuration and its actual spending/remaining amount within the current period, used for the budget dashboard in Item 16. */
public record BudgetConfigResponse(
        Long id,
        String department,
        BudgetPeriodType periodType,
        String periodLabel,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        boolean overBudget,
        String updatedBy,
        Instant updatedAt) {}
