package com.tungdt.microservices.background.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {
    @Mock
    private JavaMailSender mailSender;

    private EmailDispatchService emailDispatchService;

    @BeforeEach
    void setUp() {
        emailDispatchService = new EmailDispatchService(mailSender, new ObjectMapper(), "sender@example.com");
    }

    @Test
    void sendDeliversParsedEmail() {
        emailDispatchService.send("""
                {"to":"dotung318@gmail.com","subject":"Daily summary","body":"Body text"}
                """);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("sender@example.com");
        assertThat(message.getTo()).containsExactly("dotung318@gmail.com");
        assertThat(message.getSubject()).isEqualTo("Daily summary");
        assertThat(message.getText()).isEqualTo("Body text");
    }

    @Test
    void sendRejectsInvalidPayload() {
        assertThatThrownBy(() -> emailDispatchService.send("{\"to\":\"\",\"subject\":\"\",\"body\":\"\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
