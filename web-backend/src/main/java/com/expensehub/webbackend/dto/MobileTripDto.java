package com.expensehub.webbackend.dto;

import java.math.BigDecimal;

/**
 * Shape of a trip as returned by the Mobile backend's GET /api/trips endpoint.
 * NOTE: employeeName/department are ASSUMED to be embedded (Mobile's own
 * Android
 * app also needs to show whose trip it is). Confirm the real response shape via
 * Mobile's Swagger UI (http://localhost:8080/swagger-ui.html) and adjust only
 * this record + MobileApiClient's mapping if it differs.
 */
public record MobileTripDto(
        Long id,
        Long userId,
        String employeeName,
        String department,
        String destination,
        String startDate,
        String endDate,
        BigDecimal budgetTotal,
        String status) {
}