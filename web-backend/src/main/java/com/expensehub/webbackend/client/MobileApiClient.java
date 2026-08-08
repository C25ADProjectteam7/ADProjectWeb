package com.expensehub.webbackend.client;

import com.expensehub.webbackend.dto.MobileApiResponse;
import com.expensehub.webbackend.dto.MobileExpenseDto;
import com.expensehub.webbackend.dto.MobileTripDto;
import com.expensehub.webbackend.dto.MobileUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Talks to Mobile's company-wide admin endpoints (/api/admin/trips,
 * /api/admin/expenses) — NOT the per-user /api/trips, /api/expenses, which
 * only return the authenticated caller's own records.
 * Every response is wrapped in Mobile's ApiResponse<T> shape
 * (code/message/data).
 * Authentication is attached automatically via the "mobileRestTemplate" bean
 * (see MobileApiClientConfig) — currently a placeholder until the shared
 * JWT secret is confirmed (see that class's TODO).
 */
@Component
public class MobileApiClient {

    private final RestTemplate restTemplate;
    private final String mobileBaseUrl;

    public MobileApiClient(RestTemplate mobileRestTemplate,
            @Value("${app.mobile.base-url}") String mobileBaseUrl) {
        this.restTemplate = mobileRestTemplate;
        this.mobileBaseUrl = mobileBaseUrl;
    }

    public List<MobileTripDto> fetchAllTrips() {
        ResponseEntity<MobileApiResponse<List<MobileTripDto>>> response = restTemplate.exchange(
                mobileBaseUrl + "/api/admin/trips",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        return unwrap(response);
    }

    public List<MobileExpenseDto> fetchAllExpenses() {
        ResponseEntity<MobileApiResponse<List<MobileExpenseDto>>> response = restTemplate.exchange(
                mobileBaseUrl + "/api/admin/expenses",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        return unwrap(response);
    }

    public MobileUserDto fetchUser(Long userId) {
        ResponseEntity<MobileApiResponse<MobileUserDto>> response = restTemplate.exchange(
                mobileBaseUrl + "/api/users/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        return response.getBody() != null ? response.getBody().data() : null;
    }

    /**
     * Approve an expense via Mobile's /api/admin/expenses/approve. Body shape
     * is { expenseId, opinion } per Mobile's AdminController — note this is
     * "opinion", not "note".
     */
    public void approveExpense(Long expenseId, String opinion) {
        var body = new java.util.HashMap<String, Object>();
        body.put("expenseId", expenseId);
        body.put("opinion", opinion);
        restTemplate.postForObject(mobileBaseUrl + "/api/admin/expenses/approve", body, MobileApiResponse.class);
    }

    public void rejectExpense(Long expenseId, String opinion) {
        var body = new java.util.HashMap<String, Object>();
        body.put("expenseId", expenseId);
        body.put("opinion", opinion);
        restTemplate.postForObject(mobileBaseUrl + "/api/admin/expenses/reject", body, MobileApiResponse.class);
    }

    private <T> List<T> unwrap(ResponseEntity<MobileApiResponse<List<T>>> response) {
        if (response.getBody() == null || response.getBody().data() == null)
            return List.of();
        return response.getBody().data();
    }
}