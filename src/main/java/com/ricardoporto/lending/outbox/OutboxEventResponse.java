package com.ricardoporto.lending.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
    UUID id,
    String eventType,
    String aggregateType,
    String aggregateId,
    String payload,
    OutboxStatus status,
    int attempts,
    Instant availableAt,
    Instant createdAt,
    Instant publishedAt,
    String lastError) {
  static OutboxEventResponse from(OutboxEvent event) {
    return new OutboxEventResponse(
        event.getId(),
        event.getEventType(),
        event.getAggregateType(),
        event.getAggregateId(),
        event.getPayload(),
        event.getStatus(),
        event.getAttempts(),
        event.getAvailableAt(),
        event.getCreatedAt(),
        event.getPublishedAt(),
        event.getLastError());
  }
}
