package com.expensehub.webbackend.dto;

import java.math.BigDecimal;

/**
 * Shape of an expense as returned by Mobile's ExpenseDTO. Confirmed against
 * mobile-common/dto/ExpenseDTO.java.
 */
public record MobileExpenseDto(
        Long id,
        Long tripId,
        Long userId,
        String category,
        BigDecimal amount,
        String currency,
        String description,
        String receiptUrl,
        String status,
        String submittedAt,
        String createdAt,
        String approvalOpinion,
        String approverName) {
}