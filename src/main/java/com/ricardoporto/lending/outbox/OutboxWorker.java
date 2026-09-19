package com.ricardoporto.lending.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWorker {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxWorker.class);

  private final OutboxEventRepository eventRepository;
  private final NotificationDeliveryRepository deliveryRepository;
  private final NotificationDelivery delivery;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration retryBase;

  public OutboxWorker(
      OutboxEventRepository eventRepository,
      NotificationDeliveryRepository deliveryRepository,
      NotificationDelivery delivery,
      @Value("${api.outbox.batch-size:25}") int batchSize,
      @Value("${api.outbox.max-attempts:5}") int maxAttempts,
      @Value("${api.outbox.retry-base:PT30S}") Duration retryBase) {
    this.eventRepository = eventRepository;
    this.deliveryRepository = deliveryRepository;
    this.delivery = delivery;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.retryBase = retryBase;
  }

  @Scheduled(fixedDelayString = "${api.outbox.poll-ms:5000}")
  @Transactional
  public void processBatch() {
    var events =
        eventRepository.lockDeliveryBatch(
            OutboxStatus.PENDING, Instant.now(), PageRequest.of(0, batchSize));
    events.forEach(this::process);
  }

  private void process(OutboxEvent event) {
    var channel = delivery.channel();
    if (deliveryRepository.existsByOutboxEventIdAndChannel(event.getId(), channel)) {
      markPublished(event);
      return;
    }
    try {
      delivery.deliver(event);
      var record = new NotificationDeliveryRecord();
      record.setId(UUID.randomUUID());
      record.setOutboxEventId(event.getId());
      record.setChannel(channel);
      record.setDestination(delivery.destination(event));
      record.setDeliveredAt(Instant.now());
      deliveryRepository.save(record);
      markPublished(event);
    } catch (RuntimeException exception) {
      registerFailure(event, exception);
    }
  }

  private void markPublished(OutboxEvent event) {
    event.setStatus(OutboxStatus.PUBLISHED);
    event.setPublishedAt(Instant.now());
    event.setLastError(null);
  }

  private void registerFailure(OutboxEvent event, RuntimeException exception) {
    var attempts = event.getAttempts() + 1;
    event.setAttempts(attempts);
    event.setLastError(truncate(exception.getMessage()));
    if (attempts >= maxAttempts) {
      event.setStatus(OutboxStatus.FAILED);
      LOGGER.error("outbox_delivery_exhausted eventId={} attempts={}", event.getId(), attempts);
      return;
    }
    long multiplier = 1L << Math.min(attempts - 1, 20);
    event.setAvailableAt(Instant.now().plus(retryBase.multipliedBy(multiplier)));
    LOGGER.warn("outbox_delivery_retry eventId={} attempt={}", event.getId(), attempts);
  }

  private String truncate(String message) {
    var safe = message == null ? "Unknown delivery error" : message;
    return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
  }
}
