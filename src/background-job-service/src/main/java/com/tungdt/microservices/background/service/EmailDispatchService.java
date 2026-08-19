package com.tungdt.microservices.background.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.background.dto.EmailPayload;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailDispatchService {
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final String fromAddress;

    public EmailDispatchService(JavaMailSender mailSender, ObjectMapper objectMapper,
            @Value("${app.mail.from:${SPRING_MAIL_USERNAME:}}") String fromAddress) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.fromAddress = fromAddress;
    }

    public void send(String payload) {
        EmailPayload email = parse(payload);
        validate(email);

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }
        message.setTo(email.to());
        message.setSubject(email.subject());
        message.setText(email.body());

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new IllegalStateException("Cannot send email", ex);
        }
    }

    private EmailPayload parse(String payload) {
        try {
            return objectMapper.readValue(payload, EmailPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid email payload", ex);
        }
    }

    private void validate(EmailPayload email) {
        Objects.requireNonNull(email, "email");
        if (!StringUtils.hasText(email.to())
                || !StringUtils.hasText(email.subject())
                || !StringUtils.hasText(email.body())) {
            throw new IllegalArgumentException("Email payload must include to, subject and body");
        }
    }
}
