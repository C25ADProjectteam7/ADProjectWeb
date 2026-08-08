package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.BudgetPeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**  backlog Item 16*/
public record BudgetConfigRequest(
        @NotBlank String department,
        @NotNull BudgetPeriodType periodType,
        @NotBlank String periodLabel,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amount) {}
