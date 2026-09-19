package com.ricardoporto.lending.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
  Optional<IdempotencyRecord> findByIdempotencyKeyAndUserIdAndEndpoint(
      String idempotencyKey, Long userId, String endpoint);

  long deleteByExpiresAtBefore(Instant cutoff);
}
