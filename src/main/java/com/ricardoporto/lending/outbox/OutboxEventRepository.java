package com.ricardoporto.lending.outbox;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select event from OutboxEvent event where event.status = :status "
          + "and event.availableAt <= :now order by event.createdAt")
  List<OutboxEvent> lockDeliveryBatch(
      @Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);
}
