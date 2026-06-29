package com.tungdt.microservices.ordering.service;

import com.tungdt.microservices.ordering.dto.OutboxEventResponse;
import com.tungdt.microservices.ordering.dto.OutboxRetryResponse;
import com.tungdt.microservices.ordering.entity.OutboxEventStatus;
import com.tungdt.microservices.ordering.outbox.OutboxRelayService;
import com.tungdt.microservices.ordering.repository.OutboxEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OutboxOperationsService {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayService outboxRelayService;

    public OutboxOperationsService(OutboxEventRepository outboxEventRepository,
            OutboxRelayService outboxRelayService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxRelayService = outboxRelayService;
    }

    public List<OutboxEventResponse> getFailedEvents() {
        return outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name())
                .stream()
                .map(OutboxEventResponse::from)
                .toList();
    }

    public OutboxRetryResponse retryFailedEvents() {
        int resetCount = outboxRelayService.resetFailedEvents();
        outboxRelayService.publishPendingEvents();
        return new OutboxRetryResponse(resetCount);
    }
}
