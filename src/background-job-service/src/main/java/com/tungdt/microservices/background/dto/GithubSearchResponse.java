package com.tungdt.microservices.background.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubSearchResponse(List<GithubSearchItem> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubSearchItem(
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            String description,
            @JsonProperty("stargazers_count") Integer stargazersCount,
            String language,
            @JsonProperty("pushed_at") OffsetDateTime pushedAt
    ) {
    }
}
