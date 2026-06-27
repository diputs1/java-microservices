package com.tungdt.microservices.ordering.client;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.security.ServiceAccessTokenProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InventoryClient {
    private final RestClient restClient;
    private final ServiceAccessTokenProvider tokenProvider;

    public InventoryClient(RestClient.Builder restClientBuilder,
            @Value("${services.inventory.url:http://localhost:5006}") String inventoryServiceUrl,
            ServiceAccessTokenProvider tokenProvider) {
        this.restClient = restClientBuilder.baseUrl(inventoryServiceUrl).build();
        this.tokenProvider = tokenProvider;
    }

    public void reserve(List<InventoryReservationItemRequest> items) {
        postReservation("/api/v1/inventory/reservations", items, "Cannot reserve inventory");
    }

    public void release(List<InventoryReservationItemRequest> items) {
        postReservation("/api/v1/inventory/reservations/release", items, "Cannot release inventory");
    }

    private void postReservation(String uri, List<InventoryReservationItemRequest> items, String errorMessage) {
        try {
            restClient.post()
                    .uri(uri)
                    .headers(headers -> tokenProvider.bearerToken().ifPresent(headers::setBearerAuth))
                    .body(new InventoryReservationRequest(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException(errorMessage, HttpStatus.BAD_GATEWAY);
        }
    }
}
