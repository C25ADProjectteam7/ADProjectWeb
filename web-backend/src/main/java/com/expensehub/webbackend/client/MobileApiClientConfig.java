package com.expensehub.webbackend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * TODO (not yet implemented): every call to Mobile's /api/admin/** endpoints
 * must carry a JWT signed with a secret Mobile trusts (see their
 * JwtTokenProvider.trusted-secrets). Mobile's SecurityConfig requires role
 * MANAGER/FINANCE/ADMIN on this path.
 * For now this interceptor is a placeholder — it attaches a hardcoded
 * "not-configured" token so the request shape is correct and callers fail
 * loudly (401) instead of silently, rather than sending no Authorization
 * header at all. Replace once the shared secret value is confirmed.
 */
@Configuration
public class MobileApiClientConfig {

    @Value("${app.mobile.service-token:not-configured}")
    private String serviceToken;

    @Bean
    public RestTemplate mobileRestTemplate(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(authInterceptor());
        return restTemplate;
    }

    private ClientHttpRequestInterceptor authInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken);
            return execution.execute(request, body);
        };
    }
}