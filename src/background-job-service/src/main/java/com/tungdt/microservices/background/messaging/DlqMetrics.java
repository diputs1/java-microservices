package com.tungdt.microservices.background.messaging;

import com.tungdt.microservices.background.config.QueueConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.io.IOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DlqMetrics implements MeterBinder {
    private final RabbitTemplate rabbitTemplate;

    public DlqMetrics(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerDepthGauge(registry, QueueConfig.ORDER_CREATED_DLQ);
        registerDepthGauge(registry, QueueConfig.EMAIL_REQUESTED_DLQ);
    }

    int queueDepth(String queueName) {
        Integer messageCount = rabbitTemplate.execute(channel -> {
            try {
                return channel.queueDeclarePassive(queueName).getMessageCount();
            } catch (IOException ex) {
                return 0;
            }
        });
        return messageCount == null ? 0 : messageCount;
    }

    private void registerDepthGauge(MeterRegistry registry, String queueName) {
        Gauge.builder("dlq.depth", this, metrics -> metrics.queueDepth(queueName))
                .description("Number of messages currently in a dead-letter queue")
                .tag("queue", queueName)
                .register(registry);
    }
}
