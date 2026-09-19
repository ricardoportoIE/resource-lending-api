package com.ricardoporto.lending.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxWorkerTest {

  @Test
  void retriesWithTheSameEventIdentityWhenDeliveryConfirmationCannotBePersisted() {
    var processing = mock(OutboxEventProcessor.class);
    var delivery = mock(NotificationDelivery.class);
    var event = new OutboxEvent();
    event.setId(UUID.randomUUID());
    event.setPayload("{\"message\":\"delivered\"}");
    var firstClaim = new ClaimedOutboxEvent(event, UUID.randomUUID());
    var secondClaim = new ClaimedOutboxEvent(event, UUID.randomUUID());
    var persistenceFailure = new IllegalStateException("database unavailable after delivery");
    when(processing.claimNext())
        .thenReturn(Optional.of(firstClaim), Optional.of(secondClaim), Optional.empty());
    when(delivery.channel()).thenReturn("WEBHOOK");
    when(delivery.destination(event)).thenReturn("https://notifications.example.test");
    doNothing().when(delivery).deliver(event);
    doThrow(persistenceFailure)
        .doNothing()
        .when(processing)
        .markDelivered(any(), eq("WEBHOOK"), eq("https://notifications.example.test"));
    var worker = new OutboxWorker(processing, delivery, 1);

    worker.processBatch();
    worker.processBatch();

    verify(delivery, times(2)).deliver(event);
    verify(processing).markFailed(firstClaim, persistenceFailure);
    verify(processing).markDelivered(secondClaim, "WEBHOOK", "https://notifications.example.test");
  }
}
