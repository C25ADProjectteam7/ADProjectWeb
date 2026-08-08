package com.expensehub.webbackend.dto;

import java.math.BigDecimal;

/**
 * Shape of a trip as returned by Mobile's TripDTO. Confirmed against
 * mobile-common/dto/TripDTO.java.
 */
public record MobileTripDto(
        Long id,
        Long userId,
        String title,
        String destination,
        String startDate,
        String endDate,
        BigDecimal budgetTotal,
        String status,
        String createdAt) {
}