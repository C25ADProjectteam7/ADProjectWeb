package com.expensehub.webbackend.dto;

/** Mirrors Mobile's ApiResponse<T> wrapper: { code, message, data }. */
public record MobileApiResponse<T>(int code, String message, T data) {
}