package com.tungdt.microservices.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.email.config.QueueConfig;
import com.tungdt.microservices.email.dto.EmailRequest;
import com.tungdt.microservices.email.dto.EmailResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EmailService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public EmailResponse send(EmailRequest request) {
        try {
            log.info("Queue email to={}", request.to());
            String payload = objectMapper.writeValueAsString(request);
            String messageId = UUID.randomUUID().toString();
            rabbitTemplate.convertAndSend(QueueConfig.EMAIL_REQUESTED_QUEUE, payload, message -> {
                MessageProperties properties = message.getMessageProperties();
                properties.setMessageId(messageId);
                properties.setCorrelationId(messageId);
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setHeader("eventId", messageId);
                properties.setHeader("eventType", "EMAIL_REQUESTED");
                return message;
            });
            return new EmailResponse("QUEUED", QueueConfig.EMAIL_REQUESTED_QUEUE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot queue email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
