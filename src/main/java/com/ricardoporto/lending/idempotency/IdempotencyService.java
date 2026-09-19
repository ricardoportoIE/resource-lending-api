package com.ricardoporto.lending.idempotency;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {
  private final IdempotencyRecordRepository repository;
  private final JdbcTemplate jdbcTemplate;
  private final Duration retention;

  public IdempotencyService(
      IdempotencyRecordRepository repository,
      JdbcTemplate jdbcTemplate,
      @Value("${api.idempotency.retention:P1D}") Duration retention) {
    this.repository = repository;
    this.jdbcTemplate = jdbcTemplate;
    this.retention = retention;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IdempotencyClaim claim(String key, long userId, String endpoint, String requestHash) {
    var now = Instant.now();
    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO idempotency_records (
                id, idempotency_key, user_id, endpoint, request_hash, status,
                created_at, expires_at
            ) VALUES (?, ?, ?, ?, ?, 'PROCESSING', ?, ?)
            ON CONFLICT (idempotency_key, user_id, endpoint) DO NOTHING
            """,
            UUID.randomUUID(),
            key,
            userId,
            endpoint,
            requestHash,
            Timestamp.from(now),
            Timestamp.from(now.plus(retention)));
    var record =
        repository
            .findByIdempotencyKeyAndUserIdAndEndpoint(key, userId, endpoint)
            .orElseThrow(() -> new IllegalStateException("Idempotency claim could not be read."));
    if (inserted == 1) return new IdempotencyClaim(IdempotencyClaim.Outcome.ACQUIRED, record);
    if (!record.getRequestHash().equals(requestHash)) {
      return new IdempotencyClaim(IdempotencyClaim.Outcome.CONFLICT, record);
    }
    return new IdempotencyClaim(
        record.getStatus() == IdempotencyStatus.COMPLETED
            ? IdempotencyClaim.Outcome.REPLAY
            : IdempotencyClaim.Outcome.IN_PROGRESS,
        record);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(
      UUID id, int httpStatus, String responseBody, String contentType, String location) {
    var record = repository.findById(id).orElseThrow();
    record.setStatus(IdempotencyStatus.COMPLETED);
    record.setHttpStatus(httpStatus);
    record.setResponseBody(responseBody);
    record.setContentType(contentType);
    record.setLocation(location);
    record.setCompletedAt(Instant.now());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void release(UUID id) {
    repository.deleteById(id);
  }

  @Scheduled(fixedDelayString = "${api.idempotency.cleanup-ms:3600000}")
  @Transactional
  public void removeExpired() {
    repository.deleteByExpiresAtBefore(Instant.now());
  }
}
