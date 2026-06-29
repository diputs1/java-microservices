package com.tungdt.microservices.background.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.background.config.QueueConfig;
import com.tungdt.microservices.background.dto.DlqReplayResponse;
import com.tungdt.microservices.common.error.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DlqReplayServiceTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    private DlqReplayService dlqReplayService;

    @BeforeEach
    void setUp() {
        dlqReplayService = new DlqReplayService(rabbitTemplate, new SimpleMeterRegistry());
    }

    @Test
    void replayMovesMessagesFromOrderDlqBackToEventsExchange() {
        when(rabbitTemplate.receiveAndConvert(QueueConfig.ORDER_CREATED_DLQ))
                .thenReturn("payload-1")
                .thenReturn("payload-2")
                .thenReturn(null);

        DlqReplayResponse response = dlqReplayService.replay(QueueConfig.ORDER_CREATED_DLQ, 10);

        assertThat(response.queue()).isEqualTo(QueueConfig.ORDER_CREATED_DLQ);
        assertThat(response.replayedCount()).isEqualTo(2);
        verify(rabbitTemplate).convertAndSend(QueueConfig.EVENTS_EXCHANGE, QueueConfig.ORDER_CREATED_QUEUE,
                "payload-1");
        verify(rabbitTemplate).convertAndSend(QueueConfig.EVENTS_EXCHANGE, QueueConfig.ORDER_CREATED_QUEUE,
                "payload-2");
    }

    @Test
    void replayRejectsUnsupportedQueue() {
        assertThatThrownBy(() -> dlqReplayService.replay("unknown.dlq", 10))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(rabbitTemplate, never()).receiveAndConvert("unknown.dlq");
    }

    @Test
    void replayRejectsInvalidLimit() {
        assertThatThrownBy(() -> dlqReplayService.replay(QueueConfig.EMAIL_REQUESTED_DLQ, 0))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(rabbitTemplate, never()).receiveAndConvert(QueueConfig.EMAIL_REQUESTED_DLQ);
    }
}
