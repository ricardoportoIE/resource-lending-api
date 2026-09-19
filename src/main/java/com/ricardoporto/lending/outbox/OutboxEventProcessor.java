package com.ricardoporto.lending.outbox;

import java.util.Optional;

public interface OutboxEventProcessor {
  Optional<ClaimedOutboxEvent> claimNext();

  void markDelivered(ClaimedOutboxEvent claimed, String channel, String destination);

  void markFailed(ClaimedOutboxEvent claimed, RuntimeException exception);
}
