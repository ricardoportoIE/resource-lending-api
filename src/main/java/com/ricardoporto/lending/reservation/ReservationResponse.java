package com.ricardoporto.lending.reservation;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    Long userId,
    UUID resourceId,
    String resourceName,
    UUID readyItemId,
    ReservationStatus status,
    long queuePosition,
    Instant createdAt,
    Instant readyAt,
    Instant expiresAt) {}
