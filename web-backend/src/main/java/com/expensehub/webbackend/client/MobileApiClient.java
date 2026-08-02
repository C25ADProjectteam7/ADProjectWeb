package com.expensehub.webbackend.client;

import com.expensehub.webbackend.dto.MobileExpenseDto;
import com.expensehub.webbackend.dto.MobileTripDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Talks to the Mobile team's backend over plain HTTP. Web and Mobile are two
 * independent Spring Boot services with two independent MySQL databases, so no
 * cross-database SQL join is possible. This client is the only place in the Web
 * backend that knows Mobile's API URL and response shape.
 */
@Component
public class MobileApiClient {

    private final RestTemplate restTemplate;
    private final String mobileBaseUrl;

    public MobileApiClient(RestTemplate restTemplate,
            @Value("${app.mobile.base-url}") String mobileBaseUrl) {
        this.restTemplate = restTemplate;
        this.mobileBaseUrl = mobileBaseUrl;
    }

    public List<MobileTripDto> fetchAllTrips() {
        MobileTripDto[] trips = restTemplate.getForObject(mobileBaseUrl + "/api/trips", MobileTripDto[].class);
        return trips == null ? List.of() : List.of(trips);
    }

    public List<MobileExpenseDto> fetchAllExpenses() {
        MobileExpenseDto[] expenses = restTemplate.getForObject(mobileBaseUrl + "/api/expenses",
                MobileExpenseDto[].class);
        return expenses == null ? List.of() : List.of(expenses);
    }
}