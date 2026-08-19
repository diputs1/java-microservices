package com.tungdt.microservices.background.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.background.config.GithubSummaryProperties;
import com.tungdt.microservices.background.dto.GithubRepositorySummary;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DailyGithubSummarySchedulerTest {
    @Mock
    private GithubRepositoryClient githubRepositoryClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private DailyGithubSummaryScheduler scheduler;
    private GithubSummaryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GithubSummaryProperties();
        properties.setRecipient("dotung318@gmail.com");
        properties.setQuery("ai developer code assistant");
        properties.setLimit(3);
        scheduler = new DailyGithubSummaryScheduler(properties, githubRepositoryClient, rabbitTemplate,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void queueDailySummaryPublishesEmailPayload() {
        when(githubRepositoryClient.searchTopRepositories()).thenReturn(List.of(
                new GithubRepositorySummary(
                        "owner/repo",
                        "https://github.com/owner/repo",
                        "AI coding helper",
                        42,
                        "Java",
                        OffsetDateTime.parse("2026-08-18T12:00:00Z"))
        ));

        scheduler.queueDailySummary();

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                routingKeyCaptor.capture(),
                payloadCaptor.capture(),
                any(MessagePostProcessor.class)
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("email.requested");
        assertThat(payloadCaptor.getValue().toString()).contains("dotung318@gmail.com");
        assertThat(payloadCaptor.getValue().toString()).contains("owner/repo");
        assertThat(payloadCaptor.getValue().toString()).contains("AI coding helper");
    }

    @Test
    void queueDailySummarySkipsWhenDisabled() {
        properties.setEnabled(false);

        scheduler.queueDailySummary();

        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class),
                any(Object.class),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void queueDailySummarySkipsWhenRecipientMissing() {
        properties.setRecipient("");

        scheduler.queueDailySummary();

        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class),
                any(Object.class),
                any(MessagePostProcessor.class)
        );
    }
}
