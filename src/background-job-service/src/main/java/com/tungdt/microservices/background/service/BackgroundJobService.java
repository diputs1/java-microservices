package com.tungdt.microservices.background.service;

import com.tungdt.microservices.background.dto.JobEventResponse;
import com.tungdt.microservices.background.entity.JobEventEntity;
import com.tungdt.microservices.background.repository.JobEventRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class BackgroundJobService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundJobService.class);
    private final JobEventRepository jobEventRepository;

    public BackgroundJobService(JobEventRepository jobEventRepository) {
        this.jobEventRepository = jobEventRepository;
    }

    @RabbitListener(queues = "order.created")
    public void handleOrderCreated(String payload) {
        saveEvent("ORDER_CREATED", payload);
    }

    @RabbitListener(queues = "email.requested")
    public void handleEmailRequested(String payload) {
        saveEvent("EMAIL_REQUESTED", payload);
    }

    public List<JobEventResponse> getAll() {
        return jobEventRepository.findAll().stream().map(this::toResponse).toList();
    }

    private void saveEvent(String type, String payload) {
        log.info("Receive job event type={} payload={}", type, payload);
        JobEventEntity event = new JobEventEntity();
        event.setType(type);
        event.setPayload(payload);
        event.setReceivedAt(Instant.now());
        jobEventRepository.save(event);
    }

    private JobEventResponse toResponse(JobEventEntity event) {
        return new JobEventResponse(event.getId(), event.getType(), event.getPayload(), event.getReceivedAt());
    }
}
