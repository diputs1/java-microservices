package com.tungdt.microservices.background.repository;

import com.tungdt.microservices.background.entity.JobEventEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobEventRepository extends MongoRepository<JobEventEntity, String> {
    boolean existsByEventId(String eventId);
}
