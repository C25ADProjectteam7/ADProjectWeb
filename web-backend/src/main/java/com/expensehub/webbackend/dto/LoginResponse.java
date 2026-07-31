package com.expensehub.webbackend.dto;

public record LoginResponse(String accessToken, String role, String fullName) {}
