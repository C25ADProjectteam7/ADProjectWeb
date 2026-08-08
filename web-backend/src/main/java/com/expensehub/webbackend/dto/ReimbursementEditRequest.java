package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.ReimbursementCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 对应 backlog Item 19：财务人员手动编辑/修正已提交的报销请求。 */
public record ReimbursementEditRequest(
        @NotBlank String department,
        @NotNull ReimbursementCategory category,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        String currency,
        @NotNull LocalDate expenseDate,
        boolean receiptAttached,
        String description) {}
