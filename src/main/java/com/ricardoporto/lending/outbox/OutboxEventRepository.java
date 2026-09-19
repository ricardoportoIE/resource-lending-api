package com.ricardoporto.lending.outbox;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  Optional<OutboxEvent> findByDeduplicationKey(String deduplicationKey);

  Page<OutboxEvent> findAllByStatusOrderByCreatedAtDesc(OutboxStatus status, Pageable pageable);

  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE status IN ('PENDING', 'PROCESSING')
            AND available_at <= :now
          ORDER BY created_at
          LIMIT 1
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  Optional<OutboxEvent> lockNextDeliverable(@Param("now") Instant now);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select event from OutboxEvent event where event.id = :id")
  Optional<OutboxEvent> lockById(@Param("id") UUID id);
}
