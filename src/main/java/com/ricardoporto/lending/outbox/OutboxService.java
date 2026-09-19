package com.ricardoporto.lending.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
  private final OutboxEventRepository repository;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  public OutboxService(
      OutboxEventRepository repository, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  public OutboxEvent publish(
      String eventType,
      String aggregateType,
      Object aggregateId,
      Map<String, ?> payload,
      String deduplicationKey) {
    var existing = repository.findByDeduplicationKey(deduplicationKey);
    if (existing.isPresent()) return existing.get();

    var id = UUID.randomUUID();
    var now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO outbox_events (
            id, event_type, aggregate_type, aggregate_id, payload, status,
            attempts, available_at, created_at, deduplication_key
        ) VALUES (?, ?, ?, ?, ?, 'PENDING', 0, ?, ?, ?)
        ON CONFLICT (deduplication_key) DO NOTHING
        """,
        id,
        eventType,
        aggregateType,
        String.valueOf(aggregateId),
        toJson(payload),
        Timestamp.from(now),
        Timestamp.from(now),
        deduplicationKey);
    return repository
        .findByDeduplicationKey(deduplicationKey)
        .orElseThrow(() -> new IllegalStateException("The outbox event could not be persisted."));
  }

  private String toJson(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("The outbox payload could not be serialized.", exception);
    }
  }
}
