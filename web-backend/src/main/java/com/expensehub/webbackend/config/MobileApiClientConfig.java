package com.expensehub.webbackend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MobileApiClientConfig {

    @Bean
    public RestClient mobileApiRestClient(
            @Value("${mobile.api.base-url}") String baseUrl,
            @Value("${mobile.api.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${mobile.api.read-timeout-ms:8000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
