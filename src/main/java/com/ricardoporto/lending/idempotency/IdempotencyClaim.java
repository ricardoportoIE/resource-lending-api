package com.ricardoporto.lending.idempotency;

public record IdempotencyClaim(Outcome outcome, IdempotencyRecord record) {
  public enum Outcome {
    ACQUIRED,
    REPLAY,
    CONFLICT,
    IN_PROGRESS
  }
}
