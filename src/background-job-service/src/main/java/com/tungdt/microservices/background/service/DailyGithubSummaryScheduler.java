package com.tungdt.microservices.background.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.background.config.GithubSummaryProperties;
import com.tungdt.microservices.background.config.QueueConfig;
import com.tungdt.microservices.background.dto.EmailPayload;
import com.tungdt.microservices.background.dto.GithubRepositorySummary;
import com.tungdt.microservices.common.web.TraceHeaders;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DailyGithubSummaryScheduler {
    private static final Logger log = LoggerFactory.getLogger(DailyGithubSummaryScheduler.class);

    private final GithubSummaryProperties properties;
    private final GithubRepositoryClient githubRepositoryClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public DailyGithubSummaryScheduler(GithubSummaryProperties properties, GithubRepositoryClient githubRepositoryClient,
            RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.githubRepositoryClient = githubRepositoryClient;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${app.github-summary.cron:0 0 9 * * *}", zone = "${app.github-summary.zone:UTC}")
    public void queueDailySummary() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<GithubRepositorySummary> repositories = githubRepositoryClient.searchTopRepositories();
        String eventId = "github-summary-" + today;
        String payload = toPayload(today, repositories);

        rabbitTemplate.convertAndSend(QueueConfig.EMAIL_REQUESTED_QUEUE, payload, message -> {
            MessageProperties messageProperties = message.getMessageProperties();
            messageProperties.setMessageId(eventId);
            messageProperties.setCorrelationId(eventId);
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            messageProperties.setHeader("eventId", eventId);
            messageProperties.setHeader("eventType", "EMAIL_REQUESTED");
            messageProperties.setHeader(TraceHeaders.TRACE_ID, eventId);
            return message;
        });
        log.info("Queued daily GitHub summary for {}", properties.getRecipient());
    }

    private String toPayload(LocalDate today, List<GithubRepositorySummary> repositories) {
        String subject = "Daily GitHub AI repository summary - " + today.format(DateTimeFormatter.ISO_DATE);
        String body = buildBody(today, repositories);

        try {
            return objectMapper.writeValueAsString(new EmailPayload(properties.getRecipient(), subject, body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot build GitHub summary email", ex);
        }
    }

    private String buildBody(LocalDate today, List<GithubRepositorySummary> repositories) {
        StringBuilder body = new StringBuilder()
                .append("Daily GitHub summary for ")
                .append(today.format(DateTimeFormatter.ISO_DATE))
                .append('\n')
                .append("Focus: AI repositories that help developers write code.")
                .append('\n')
                .append("Search query: ")
                .append(properties.getQuery())
                .append("\n\n");

        if (repositories.isEmpty()) {
            body.append("No matching repositories were found today.");
            return body.toString();
        }

        int index = 1;
        for (GithubRepositorySummary repository : repositories) {
            body.append(index++)
                    .append(". ")
                    .append(repository.fullName())
                    .append('\n')
                    .append("   URL: ")
                    .append(repository.htmlUrl())
                    .append('\n')
                    .append("   Stars: ")
                    .append(repository.stargazersCount())
                    .append('\n')
                    .append("   Language: ")
                    .append(repository.language() == null ? "Unknown" : repository.language())
                    .append('\n')
                    .append("   Last push: ")
                    .append(repository.pushedAt() == null ? "Unknown" : repository.pushedAt())
                    .append('\n')
                    .append("   Summary: ")
                    .append(repository.description() == null ? "No description provided." : repository.description())
                    .append("\n\n");
        }

        return body.toString().trim();
    }
}
