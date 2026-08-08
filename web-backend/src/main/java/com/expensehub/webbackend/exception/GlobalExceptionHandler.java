package com.expensehub.webbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", ex.getMessage()));
    }


    @ExceptionHandler(com.expensehub.webbackend.integration.mobile.MobileApiException.class)
    public ResponseEntity<ApiError> handleMobileApiException(
            com.expensehub.webbackend.integration.mobile.MobileApiException ex) {
        int status = ex.getUpstreamStatus();
        if (status >= 400 && status < 500) {
            return ResponseEntity.status(status)
                    .body(ApiError.of(status, "Request Rejected By Mobile Service", ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(
                        ApiError.of(
                                502,
                                "Bad Gateway",
                                "Unable to obtain reimbursement data from Mobile service, please try again later:" + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error", ex.getMessage()));
    }
}
