package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reimbursement request details. The data itself comes from the Mobile backend (GET /api/admin/expenses and other endpoints),
 * while the Web only fetches, supplements by department, calculates compliance flags, filters, and paginates before displaying,
 * without persisting a local copy.
 * <p>policyFlags are calculated in real-time, not stored in the database, and will be automatically refreshed when budgets are adjusted.
 */
public record ReimbursementResponse(
        Long id,
        String employeeName,
        Long employeeUserId,
        String department,
        ReimbursementCategory category,
        BigDecimal amount,
        String currency,
        String description,
        boolean receiptAttached,
        String receiptUrl,
        ReimbursementStatus status,
        LocalDateTime submittedAt,
        List<String> policyFlags,
        String reviewComment,
        String reviewedBy) {}
