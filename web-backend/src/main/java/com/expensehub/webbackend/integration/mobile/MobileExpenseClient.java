package com.expensehub.webbackend.integration.mobile;

import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// The sole entry point for the finance module to interact with the Mobile backend.
// Also used by the Manager approval module (Item 20) and analytics module (Item 23-25)
// to read Mobile's trip data (GET /api/admin/trips), so there's one shared,
// authenticated client instead of separate unauthenticated placeholder ones.

@Component
public class MobileExpenseClient {

    private final RestClient restClient;
    private final MobileAuthTokenProvider tokenProvider;

    public MobileExpenseClient(RestClient mobileApiRestClient, MobileAuthTokenProvider tokenProvider) {
        this.restClient = mobileApiRestClient;
        this.tokenProvider = tokenProvider;
    }

    public List<MobileExpenseDTO> listAllExpenses() {
        MobileApiResponse<List<MobileExpenseDTO>> response =
                get("/api/admin/expenses", new ParameterizedTypeReference<>() {});
        return response.getData() != null ? response.getData() : List.of();
    }

    /** Company-wide trip list, for the Manager approval workflow and analytics.
     * Uses the SERVICE ACCOUNT token, not the current Web user's: this is also
     * called by the @Scheduled auto-sync job, which has no logged-in user -
     * currentUserTokenForMobile() throws there (observed live in prod logs). */
    public List<MobileTripDTO> listAllTrips() {
        MobileApiResponse<List<MobileTripDTO>> response =
                get("/api/admin/trips", new ParameterizedTypeReference<>() {},
                        tokenProvider.serviceAccountTokenForMobile());
        return response.getData() != null ? response.getData() : List.of();
    }

    public MobileExpenseDTO approve(Long expenseId, String opinion) {
        MobileApiResponse<MobileExpenseDTO> response =
                post(
                        "/api/admin/expenses/approve",
                        Map.of("expenseId", expenseId, "opinion", opinion == null ? "" : opinion),
                        new ParameterizedTypeReference<>() {});
        return response.getData();
    }

    public MobileExpenseDTO reject(Long expenseId, String opinion) {
        MobileApiResponse<MobileExpenseDTO> response =
                post(
                        "/api/admin/expenses/reject",
                        Map.of("expenseId", expenseId, "opinion", opinion == null ? "" : opinion),
                        new ParameterizedTypeReference<>() {});
        return response.getData();
    }

    //Corresponding to the NEEDS_INFO status of Mobile: The reviewer requires the employee to supplement information/resubmit after modification
    public MobileExpenseDTO requestInfo(Long expenseId, String opinion) {
        MobileApiResponse<MobileExpenseDTO> response =
                post(
                        "/api/admin/expenses/request-info",
                        Map.of("expenseId", expenseId, "opinion", opinion == null ? "" : opinion),
                        new ParameterizedTypeReference<>() {});
        return response.getData();
    }

    /**
     * Looks up a Mobile user's profile (name/department) for display purposes.
     * Uses the dedicated service account token, not the current Web user's token,
     * because Mobile's UserService.getUserById() requires the JWT subject to
     * resolve to a real Mobile user record — see MobileAuthTokenProvider.
     */
    public MobileUserDTO getUser(Long userId) {
        MobileApiResponse<MobileUserDTO> response =
                get("/api/users/" + userId, new ParameterizedTypeReference<>() {},
                        tokenProvider.serviceAccountTokenForMobile());
        return response.getData();
    }

    private <T> MobileApiResponse<T> get(
            String path, ParameterizedTypeReference<MobileApiResponse<T>> type) {
        return get(path, type, tokenProvider.currentUserTokenForMobile());
    }

    private <T> MobileApiResponse<T> get(
            String path, ParameterizedTypeReference<MobileApiResponse<T>> type, String token) {
        try {
            MobileApiResponse<T> body =
                    restClient
                            .get()
                            .uri(path)
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .body(type);
            return requireSuccess(body);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw translateResponseError(path, e);
        } catch (RestClientException e) {
            throw new MobileApiException("Failed to reach Mobile API: " + path, e);
        }
    }

    private <T> MobileApiResponse<T> post(
            String path, Object requestBody, ParameterizedTypeReference<MobileApiResponse<T>> type) {
        try {
            MobileApiResponse<T> body =
                    restClient
                            .post()
                            .uri(path)
                            .header("Authorization", "Bearer " + tokenProvider.currentUserTokenForMobile())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(type);
            return requireSuccess(body);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw translateResponseError(path, e);
        } catch (RestClientException e) {
            throw new MobileApiException("Failed to reach Mobile API: " + path, e);
        }
    }


    private MobileApiException translateResponseError(
            String path, org.springframework.web.client.RestClientResponseException e) {
        String message = e.getStatusText();
        try {
            MobileApiResponse<Void> errorBody =
                    e.getResponseBodyAs(new ParameterizedTypeReference<MobileApiResponse<Void>>() {});
            if (errorBody != null && errorBody.getMessage() != null) {
                message = errorBody.getMessage();
            }
        } catch (Exception parseFailure) {
        }
        return new MobileApiException(e.getStatusCode().value(), path + " -> " + message);
    }

    private <T> MobileApiResponse<T> requireSuccess(MobileApiResponse<T> body) {
        if (body == null) {
            throw new MobileApiException(502, "Mobile API returned an empty response");
        }
        if (!body.isSuccess()) {
            throw new MobileApiException(body.getCode(), body.getMessage());
        }
        return body;
    }
}
