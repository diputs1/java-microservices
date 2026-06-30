package com.tungdt.microservices.ordering.repository;

import com.tungdt.microservices.ordering.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    Optional<OutboxEventEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    @Query(value = """
            SELECT TOP (:limit) *
            FROM outbox_events WITH (UPDLOCK, READPAST, ROWLOCK)
            WHERE status = 'PENDING'
               OR (status = 'IN_PROGRESS' AND claimed_at < :staleBefore)
            ORDER BY created_at ASC
            """, nativeQuery = true)
    List<OutboxEventEntity> findClaimableForUpdate(
            @Param("limit") int limit,
            @Param("staleBefore") Instant staleBefore
    );
}
