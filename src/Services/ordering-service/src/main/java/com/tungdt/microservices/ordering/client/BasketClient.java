package com.tungdt.microservices.ordering.client;

import com.tungdt.microservices.common.api.ApiResponse;
import com.tungdt.microservices.common.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BasketClient {
    private final RestClient restClient;

    public BasketClient(RestClient.Builder restClientBuilder,
            @Value("${services.basket.url:http://localhost:5004}") String basketServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(basketServiceUrl).build();
    }

    public BasketResponse getBasket(String customerId) {
        try {
            ApiResponse<BasketResponse> response = restClient.get()
                    .uri("/api/v1/baskets/{customerId}", customerId)
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
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException("Cannot delete basket", HttpStatus.BAD_GATEWAY);
        }
    }
}
