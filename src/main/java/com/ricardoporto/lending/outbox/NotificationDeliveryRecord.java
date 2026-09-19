package com.ricardoporto.lending.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
public class NotificationDeliveryRecord {
  @Id private UUID id;

  @Column(name = "outbox_event_id", nullable = false)
  private UUID outboxEventId;

  @Column(nullable = false)
  private String channel;

  @Column(nullable = false)
  private String destination;

  @Column(name = "delivered_at", nullable = false)
  private Instant deliveredAt;
}
