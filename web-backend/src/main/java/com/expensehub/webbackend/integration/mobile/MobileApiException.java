package com.expensehub.webbackend.integration.mobile;

/**
 * Thrown when a Mobile-api call fails (due to network errors, timeouts, or non-2xx responses returned by Mobile).
 * Converted into an error response for the frontend by GlobalExceptionHandler.
 */
public class MobileApiException extends RuntimeException {

    private final int upstreamStatus;

    public MobileApiException(int upstreamStatus, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public MobileApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 0;
    }

    /** Returns the original HTTP status code returned by the Mobile endpoint; 0 indicates a network-level failure (no response received). */
    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
