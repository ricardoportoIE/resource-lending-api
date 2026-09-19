package com.ricardoporto.lending.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest extends PostgresIntegrationTest {
  @Autowired private OutboxService outboxService;
  @Autowired private OutboxWorker worker;
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
  void publishesAndDeliversExactlyOnceForRepeatedProcessing() {
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
}
