package com.tungdt.microservices.background.service;

import com.tungdt.microservices.background.config.GithubSummaryProperties;
import com.tungdt.microservices.background.dto.GithubRepositorySummary;
import com.tungdt.microservices.background.dto.GithubSearchResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class GithubRepositoryClient {
    private final RestClient restClient;
    private final GithubSummaryProperties properties;

    public GithubRepositoryClient(RestClient.Builder restClientBuilder, GithubSummaryProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
        this.properties = properties;
    }

    public List<GithubRepositorySummary> searchTopRepositories() {
        GithubSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/repositories")
                        .queryParam("q", properties.getQuery())
                        .queryParam("sort", "updated")
                        .queryParam("order", "desc")
                        .queryParam("per_page", properties.getLimit())
                        .build())
                .headers(headers -> {
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set("X-GitHub-Api-Version", "2022-11-28");
                    if (StringUtils.hasText(properties.getToken())) {
                        headers.setBearerAuth(properties.getToken());
                    }
                })
                .retrieve()
                .body(GithubSearchResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> new GithubRepositorySummary(
                        item.fullName(),
                        item.htmlUrl(),
                        item.description(),
                        item.stargazersCount() == null ? 0 : item.stargazersCount(),
                        item.language(),
                        item.pushedAt()
                ))
                .toList();
    }
}
