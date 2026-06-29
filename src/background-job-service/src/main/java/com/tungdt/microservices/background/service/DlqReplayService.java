package com.tungdt.microservices.background.service;

import com.tungdt.microservices.background.config.QueueConfig;
import com.tungdt.microservices.background.dto.DlqReplayResponse;
import com.tungdt.microservices.common.error.BusinessException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DlqReplayService {
    private static final int DEFAULT_LIMIT = 100;

    private final RabbitTemplate rabbitTemplate;

    public DlqReplayService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public DlqReplayResponse replay(String queueName, Integer limit) {
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.from(queueName);
        int maxMessages = limit == null ? DEFAULT_LIMIT : limit;
        if (maxMessages < 1 || maxMessages > 1000) {
            throw new BusinessException("Replay limit must be between 1 and 1000", HttpStatus.BAD_REQUEST);
        }

        int replayedCount = 0;
        for (int i = 0; i < maxMessages; i++) {
            Object message = rabbitTemplate.receiveAndConvert(deadLetterQueue.dlqName());
            if (message == null) {
                break;
            }
            rabbitTemplate.convertAndSend(QueueConfig.EVENTS_EXCHANGE, deadLetterQueue.routingKey(), message);
            replayedCount++;
        }
        return new DlqReplayResponse(deadLetterQueue.dlqName(), replayedCount);
    }

    private enum DeadLetterQueue {
        ORDER_CREATED(QueueConfig.ORDER_CREATED_DLQ, QueueConfig.ORDER_CREATED_QUEUE),
        EMAIL_REQUESTED(QueueConfig.EMAIL_REQUESTED_DLQ, QueueConfig.EMAIL_REQUESTED_QUEUE);

        private final String dlqName;
        private final String routingKey;

        DeadLetterQueue(String dlqName, String routingKey) {
            this.dlqName = dlqName;
            this.routingKey = routingKey;
        }

        static DeadLetterQueue from(String queueName) {
            for (DeadLetterQueue deadLetterQueue : values()) {
                if (deadLetterQueue.dlqName.equals(queueName)) {
                    return deadLetterQueue;
                }
            }
            throw new BusinessException("Unsupported dead-letter queue", HttpStatus.BAD_REQUEST);
        }

        String dlqName() {
            return dlqName;
        }

        String routingKey() {
            return routingKey;
        }
    }
}
