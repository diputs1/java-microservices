package com.tungdt.microservices.background.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.tungdt.microservices.background.config.QueueConfig;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DlqMetricsTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @Mock
    private AMQP.Queue.DeclareOk declareOk;

    private DlqMetrics dlqMetrics;

    @BeforeEach
    void setUp() {
        dlqMetrics = new DlqMetrics(rabbitTemplate);
    }

    @Test
    void queueDepthReturnsPassiveDeclareMessageCount() throws IOException {
        when(rabbitTemplate.execute(org.mockito.ArgumentMatchers.<ChannelCallback<Integer>>any()))
                .thenAnswer(invocation -> invocation.<ChannelCallback<Integer>>getArgument(0).doInRabbit(channel));
        when(channel.queueDeclarePassive(QueueConfig.ORDER_CREATED_DLQ))
                .thenReturn(declareOk);
        when(declareOk.getMessageCount()).thenReturn(7);

        assertThat(dlqMetrics.queueDepth(QueueConfig.ORDER_CREATED_DLQ)).isEqualTo(7);
    }

    @Test
    void queueDepthReturnsZeroWhenQueueDoesNotExist() throws IOException {
        when(rabbitTemplate.execute(org.mockito.ArgumentMatchers.<ChannelCallback<Integer>>any()))
                .thenAnswer(invocation -> invocation.<ChannelCallback<Integer>>getArgument(0).doInRabbit(channel));
        when(channel.queueDeclarePassive(QueueConfig.EMAIL_REQUESTED_DLQ))
                .thenThrow(new IOException("missing queue"));

        assertThat(dlqMetrics.queueDepth(QueueConfig.EMAIL_REQUESTED_DLQ)).isZero();
    }
}
