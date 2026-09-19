package com.ricardoporto.lending.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxProcessingService implements OutboxEventProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxProcessingService.class);

  private final OutboxEventRepository eventRepository;
  private final NotificationDeliveryRepository deliveryRepository;
  private final int maxAttempts;
  private final Duration retryBase;
  private final Duration deliveryLease;

  public OutboxProcessingService(
      OutboxEventRepository eventRepository,
      NotificationDeliveryRepository deliveryRepository,
      @Value("${api.outbox.max-attempts:5}") int maxAttempts,
      @Value("${api.outbox.retry-base:PT30S}") Duration retryBase,
      @Value("${api.outbox.delivery-lease:PT30S}") Duration deliveryLease) {
    this.eventRepository = eventRepository;
    this.deliveryRepository = deliveryRepository;
    this.maxAttempts = maxAttempts;
    this.retryBase = retryBase;
    this.deliveryLease = deliveryLease;
  }

  @Transactional
  @Override
  public Optional<ClaimedOutboxEvent> claimNext() {
    while (true) {
      var now = Instant.now();
      var candidate = eventRepository.lockNextDeliverable(now);
      if (candidate.isEmpty()) return Optional.empty();

      var event = candidate.orElseThrow();
      if (event.getAttempts() >= maxAttempts) {
        event.setStatus(OutboxStatus.FAILED);
        event.setProcessingToken(null);
        event.setLastError(
            "Delivery outcome remained unknown after the maximum number of attempts.");
        eventRepository.saveAndFlush(event);
        LOGGER.error(
            "outbox_delivery_exhausted eventId={} attempts={}", event.getId(), event.getAttempts());
        continue;
      }

      var claimToken = UUID.randomUUID();
      event.setStatus(OutboxStatus.PROCESSING);
      event.setAttempts(event.getAttempts() + 1);
      event.setProcessingToken(claimToken);
      event.setAvailableAt(now.plus(deliveryLease));
      eventRepository.saveAndFlush(event);
      return Optional.of(new ClaimedOutboxEvent(event, claimToken));
    }
  }

  @Transactional
  @Override
  public void markDelivered(ClaimedOutboxEvent claimed, String channel, String destination) {
    var event = eventRepository.lockById(claimed.event().getId()).orElseThrow();
    if (event.getStatus() == OutboxStatus.PUBLISHED || !ownsClaim(event, claimed)) return;

    if (!deliveryRepository.existsByOutboxEventIdAndChannel(event.getId(), channel)) {
      var record = new NotificationDeliveryRecord();
      record.setId(UUID.randomUUID());
      record.setOutboxEventId(event.getId());
      record.setChannel(channel);
      record.setDestination(destination);
      record.setDeliveredAt(Instant.now());
      deliveryRepository.save(record);
    }
    event.setStatus(OutboxStatus.PUBLISHED);
    event.setPublishedAt(Instant.now());
    event.setLastError(null);
    event.setProcessingToken(null);
  }

  @Transactional
  @Override
  public void markFailed(ClaimedOutboxEvent claimed, RuntimeException exception) {
    var event = eventRepository.lockById(claimed.event().getId()).orElseThrow();
    if (!ownsClaim(event, claimed)) return;

    event.setLastError(truncate(exception.getMessage()));
    event.setProcessingToken(null);
    if (event.getAttempts() >= maxAttempts) {
      event.setStatus(OutboxStatus.FAILED);
      LOGGER.error(
          "outbox_delivery_exhausted eventId={} attempts={}", event.getId(), event.getAttempts());
      return;
    }
    long multiplier = 1L << Math.min(event.getAttempts() - 1, 20);
    event.setStatus(OutboxStatus.PENDING);
    event.setAvailableAt(Instant.now().plus(retryBase.multipliedBy(multiplier)));
    LOGGER.warn("outbox_delivery_retry eventId={} attempt={}", event.getId(), event.getAttempts());
  }

  private boolean ownsClaim(OutboxEvent event, ClaimedOutboxEvent claimed) {
    return event.getStatus() == OutboxStatus.PROCESSING
        && claimed.claimToken().equals(event.getProcessingToken());
  }

  private String truncate(String message) {
    var safe = message == null ? "Unknown delivery error" : message;
    return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
  }
}
