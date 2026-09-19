package com.ricardoporto.lending.outbox;

public interface NotificationDelivery {
  String channel();

  String destination(OutboxEvent event);

  void deliver(OutboxEvent event);
}
