package com.ricardoporto.lending.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {
  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxWorker.class);

  private final OutboxEventProcessor processingService;
  private final NotificationDelivery delivery;
  private final int batchSize;

  public OutboxWorker(
      OutboxEventProcessor processingService,
      NotificationDelivery delivery,
      @Value("${api.outbox.batch-size:25}") int batchSize) {
    this.processingService = processingService;
    this.delivery = delivery;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${api.outbox.poll-ms:5000}")
  public void processBatch() {
    for (var processed = 0; processed < batchSize; processed++) {
      var claimed = processingService.claimNext();
      if (claimed.isEmpty()) return;
      process(claimed.orElseThrow());
    }
  }

  private void process(ClaimedOutboxEvent claimed) {
    try {
      var channel = delivery.channel();
      var destination = delivery.destination(claimed.event());
      delivery.deliver(claimed.event());
      processingService.markDelivered(claimed, channel, destination);
    } catch (RuntimeException exception) {
      try {
        processingService.markFailed(claimed, exception);
      } catch (RuntimeException persistenceFailure) {
        LOGGER.error(
            "outbox_delivery_result_not_persisted eventId={}",
            claimed.event().getId(),
            persistenceFailure);
      }
    }
  }
}
