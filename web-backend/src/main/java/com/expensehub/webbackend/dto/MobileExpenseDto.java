package com.expensehub.webbackend.dto;

import java.math.BigDecimal;

/**
 * Shape of an expense record as returned by Mobile's GET /api/expenses
 * endpoint.
 */
public record MobileExpenseDto(
        Long id,
        Long userId,
        String employeeName,
        String department,
        String category,
        BigDecimal amount,
        String status,
        String submittedAt) {
}