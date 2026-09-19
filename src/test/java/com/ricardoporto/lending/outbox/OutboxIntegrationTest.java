package com.ricardoporto.lending.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ricardoporto.lending.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest extends PostgresIntegrationTest {
  @Autowired private OutboxService outboxService;
  @Autowired private OutboxWorker worker;
  @Autowired private OutboxProcessingService processingService;
  @Autowired private OutboxEventRepository eventRepository;
  @Autowired private NotificationDeliveryRepository deliveryRepository;
  @MockitoBean private NotificationDelivery delivery;

  @BeforeEach
  void configureDelivery() {
    deliveryRepository.deleteAll();
    eventRepository.deleteAll();
    when(delivery.channel()).thenReturn("TEST");
    when(delivery.destination(any())).thenReturn("test-sink");
  }

  @Test
  void doesNotRedeliverAnEventAfterItsPublishedStateIsCommitted() {
    var key = "TEST:" + UUID.randomUUID();
    var first = outboxService.publish("TEST_EVENT", "Test", 42, Map.of("answer", 42), key);
    var duplicate = outboxService.publish("TEST_EVENT", "Test", 42, Map.of("answer", 42), key);
    assertEquals(first.getId(), duplicate.getId());
    doNothing().when(delivery).deliver(any());

    worker.processBatch();
    worker.processBatch();

    var published = eventRepository.findById(first.getId()).orElseThrow();
    assertEquals(OutboxStatus.PUBLISHED, published.getStatus());
    assertNotNull(published.getPublishedAt());
    assertEquals(1, deliveryRepository.count());
    verify(delivery, times(1)).deliver(any());
  }

  @Test
  void commitsTheClaimBeforeCallingTheExternalDelivery() {
    var event =
        outboxService.publish(
            "TRANSACTION_TEST",
            "Test",
            43,
            Map.of("answer", 43),
            "TRANSACTION:" + UUID.randomUUID());
    var transactionActive = new AtomicBoolean(true);
    var visibleStatus = new AtomicReference<OutboxStatus>();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
              visibleStatus.set(
                  eventRepository
                      .findById(invocation.<OutboxEvent>getArgument(0).getId())
                      .orElseThrow()
                      .getStatus());
              return null;
            })
        .when(delivery)
        .deliver(any());

    worker.processBatch();

    assertFalse(transactionActive.get());
    assertEquals(OutboxStatus.PROCESSING, visibleStatus.get());
    var published = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(OutboxStatus.PUBLISHED, published.getStatus());
    assertEquals(1, published.getAttempts());
  }

  @Test
  void retriesWithBackoffAndMovesExhaustedDeliveryToFailed() {
    var event =
        outboxService.publish(
            "FAILING_EVENT", "Test", 99, Map.of("value", "failure"), "FAIL:" + UUID.randomUUID());
    doThrow(new IllegalStateException("sink unavailable")).when(delivery).deliver(any());

    for (int attempt = 1; attempt <= 5; attempt++) {
      worker.processBatch();
      var current = eventRepository.findById(event.getId()).orElseThrow();
      assertEquals(attempt, current.getAttempts());
      if (attempt < 5) {
        current.setAvailableAt(Instant.now().minusSeconds(1));
        eventRepository.save(current);
      }
    }

    var failed = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(OutboxStatus.FAILED, failed.getStatus());
    assertEquals("sink unavailable", failed.getLastError());
    assertEquals(0, deliveryRepository.count());
  }

  @Test
  void recoversAnExpiredProcessingLeaseForAtLeastOnceDelivery() {
    var event =
        outboxService.publish(
            "LEASE_RECOVERY", "Test", 100, Map.of("value", "lease"), "LEASE:" + UUID.randomUUID());
    var abandonedClaim = processingService.claimNext().orElseThrow();
    assertEquals(event.getId(), abandonedClaim.event().getId());

    var leased = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(OutboxStatus.PROCESSING, leased.getStatus());
    leased.setAvailableAt(Instant.now().minusSeconds(1));
    eventRepository.save(leased);
    doNothing().when(delivery).deliver(any());

    worker.processBatch();

    var recovered = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(OutboxStatus.PUBLISHED, recovered.getStatus());
    assertEquals(2, recovered.getAttempts());
    verify(delivery, times(1)).deliver(any());
  }

  @Test
  void staleClaimCannotOverwriteTheOutcomeOfANewerClaim() {
    var event =
        outboxService.publish(
            "CLAIM_FENCING",
            "Test",
            101,
            Map.of("value", "fenced"),
            "FENCING:" + UUID.randomUUID());
    var firstClaim = processingService.claimNext().orElseThrow();
    var leased = eventRepository.findById(event.getId()).orElseThrow();
    leased.setAvailableAt(Instant.now().minusSeconds(1));
    eventRepository.save(leased);
    var secondClaim = processingService.claimNext().orElseThrow();

    processingService.markFailed(firstClaim, new IllegalStateException("late failure"));
    var stillOwnedBySecondClaim = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(OutboxStatus.PROCESSING, stillOwnedBySecondClaim.getStatus());
    assertEquals(secondClaim.claimToken(), stillOwnedBySecondClaim.getProcessingToken());

    processingService.markDelivered(secondClaim, "TEST", "test-sink");
    assertEquals(
        OutboxStatus.PUBLISHED, eventRepository.findById(event.getId()).orElseThrow().getStatus());
  }
}
