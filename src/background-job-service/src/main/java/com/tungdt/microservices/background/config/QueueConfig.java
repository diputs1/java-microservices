package com.tungdt.microservices.background.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    public static final String EVENTS_EXCHANGE = "microservices.events";
    public static final String DEAD_LETTER_EXCHANGE = "microservices.events.dlx";
    public static final String ORDER_CREATED_QUEUE = "order.created";
    public static final String ORDER_CREATED_DLQ = "order.created.dlq";
    public static final String EMAIL_REQUESTED_QUEUE = "email.requested";
    public static final String EMAIL_REQUESTED_DLQ = "email.requested.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return durableQueue(ORDER_CREATED_QUEUE);
    }

    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return new Queue(ORDER_CREATED_DLQ, true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(eventsExchange).with(ORDER_CREATED_QUEUE);
    }

    @Bean
    public Binding orderCreatedDeadLetterBinding(Queue orderCreatedDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue)
                .to(deadLetterExchange)
                .with(ORDER_CREATED_QUEUE);
    }

    @Bean
    public Queue emailRequestedQueue() {
        return durableQueue(EMAIL_REQUESTED_QUEUE);
    }

    @Bean
    public Queue emailRequestedDeadLetterQueue() {
        return new Queue(EMAIL_REQUESTED_DLQ, true);
    }

    @Bean
    public Binding emailRequestedBinding(Queue emailRequestedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(emailRequestedQueue).to(eventsExchange).with(EMAIL_REQUESTED_QUEUE);
    }

    @Bean
    public Binding emailRequestedDeadLetterBinding(Queue emailRequestedDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailRequestedDeadLetterQueue)
                .to(deadLetterExchange)
                .with(EMAIL_REQUESTED_QUEUE);
    }

    private Queue durableQueue(String queueName) {
        return new Queue(queueName, true, false, false, Map.of(
                "x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key", queueName
        ));
    }
}
