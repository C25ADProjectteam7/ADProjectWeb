package com.expensehub.webbackend.dto;

/**
 * Shape of a user as read from Mobile's `users` table (docker/mysql/init.sql).
 * NOT YET CONFIRMED that a GET /api/users/{id} (or equivalent) endpoint
 * exists on Mobile's side to actually serve this — see
 * MobileApiClient.fetchUser().
 */
public record MobileUserDto(
        Long id,
        String username,
        String email,
        String department,
        String role) {
}