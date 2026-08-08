package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.ReimbursementCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报销请求的接入入口：员工在 App 端提交（含 OCR 识别结果，见 backlog Item 14）后，
 * 由 Mobile 服务调用本接口把请求写入 Web 端数据库，财务人员再在本模块内查看与审核。
 */
public record ReimbursementCreateRequest(
        @NotBlank String employeeName,
        @NotBlank String department,
        @NotNull ReimbursementCategory category,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        String currency,
        @NotNull LocalDate expenseDate,
        boolean receiptAttached,
        String description) {}
