package com.ricardoporto.lending.outbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository
    extends JpaRepository<NotificationDeliveryRecord, UUID> {
  boolean existsByOutboxEventIdAndChannel(UUID outboxEventId, String channel);
}
