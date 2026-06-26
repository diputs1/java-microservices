package com.tungdt.microservices.ordering.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(ServiceAccountProperties.class)
public class ServiceAccessTokenProvider {
    private final RestClient restClient;
    private final ServiceAccountProperties properties;
    private final Clock clock;
    private CachedToken cachedToken;

    public ServiceAccessTokenProvider(RestClient.Builder restClientBuilder, ServiceAccountProperties properties) {
        this(restClientBuilder, properties, Clock.systemUTC());
    }

    ServiceAccessTokenProvider(RestClient.Builder restClientBuilder,
            ServiceAccountProperties properties,
            Clock clock) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized Optional<String> bearerToken() {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (cachedToken != null && cachedToken.expiresAt().isAfter(Instant.now(clock).plusSeconds(30))) {
            return Optional.of(cachedToken.accessToken());
        }

        TokenResponse response = requestToken();
        cachedToken = new CachedToken(
                response.accessToken(),
                Instant.now(clock).plusSeconds(Math.max(30, response.expiresIn()))
        );
        return Optional.of(cachedToken.accessToken());
    }

    private boolean isConfigured() {
        return StringUtils.hasText(properties.getTokenUri())
                && StringUtils.hasText(properties.getClientId())
                && StringUtils.hasText(properties.getClientSecret());
    }

    private TokenResponse requestToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        if (StringUtils.hasText(properties.getScope())) {
            form.add("scope", properties.getScope());
        }

        return restClient.post()
                .uri(properties.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }

    private record TokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
            @com.fasterxml.jackson.annotation.JsonProperty("expires_in") long expiresIn
    ) {
    }
}
