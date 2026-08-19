package com.tungdt.microservices.background.dto;

import java.time.OffsetDateTime;

public record GithubRepositorySummary(
        String fullName,
        String htmlUrl,
        String description,
        int stargazersCount,
        String language,
        OffsetDateTime pushedAt
) {
}
