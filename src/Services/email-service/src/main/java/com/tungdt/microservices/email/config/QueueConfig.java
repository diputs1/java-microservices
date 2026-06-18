package com.tungdt.microservices.email.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    public static final String EMAIL_REQUESTED_QUEUE = "email.requested";

    @Bean
    public Queue emailRequestedQueue() {
        return new Queue(EMAIL_REQUESTED_QUEUE, true);
    }
}
