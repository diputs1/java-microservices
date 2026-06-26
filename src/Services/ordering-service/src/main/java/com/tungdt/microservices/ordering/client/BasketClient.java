package com.tungdt.microservices.ordering.client;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.security.ServiceAccessTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BasketClient {
    private final RestClient restClient;
    private final ServiceAccessTokenProvider tokenProvider;

    public BasketClient(RestClient.Builder restClientBuilder,
            @Value("${services.basket.url:http://localhost:5004}") String basketServiceUrl,
            ServiceAccessTokenProvider tokenProvider) {
        this.restClient = restClientBuilder.baseUrl(basketServiceUrl).build();
        this.tokenProvider = tokenProvider;
    }

    public BasketResponse getBasket(String customerId) {
        try {
            ApiResponse<BasketResponse> response = restClient.get()
                    .uri("/api/v1/baskets/{customerId}", customerId)
                    .headers(headers -> tokenProvider.bearerToken().ifPresent(headers::setBearerAuth))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException("Cannot load basket", HttpStatus.BAD_GATEWAY);
            }
            return response.data();
        } catch (RestClientException ex) {
            throw new BusinessException("Cannot load basket", HttpStatus.BAD_GATEWAY);
        }
    }

    public void deleteBasket(String customerId) {
        try {
            restClient.delete()
                    .uri("/api/v1/baskets/{customerId}", customerId)
                    .headers(headers -> tokenProvider.bearerToken().ifPresent(headers::setBearerAuth))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException("Cannot delete basket", HttpStatus.BAD_GATEWAY);
        }
    }
}
