package com.ricardoporto.lending.outbox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableNotificationDelivery implements NotificationDelivery {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConfigurableNotificationDelivery.class);

  private final String webhookUrl;
  private final HttpClient httpClient;

  public ConfigurableNotificationDelivery(
      @Value("${api.notifications.webhook-url:}") String webhookUrl) {
    this.webhookUrl = webhookUrl.trim();
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  }

  @Override
  public String channel() {
    return webhookUrl.isEmpty() ? "LOG" : "WEBHOOK";
  }

  @Override
  public String destination(OutboxEvent event) {
    return webhookUrl.isEmpty() ? "application-log" : webhookUrl;
  }

  @Override
  public void deliver(OutboxEvent event) {
    if (webhookUrl.isEmpty()) {
      LOGGER.info(
          "notification_delivered eventId={} eventType={} channel=LOG",
          event.getId(),
          event.getEventType());
      return;
    }
    try {
      var request =
          HttpRequest.newBuilder(URI.create(webhookUrl))
              .timeout(Duration.ofSeconds(5))
              .header("Content-Type", "application/json")
              .header("Idempotency-Key", event.getId().toString())
              .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
              .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Webhook returned HTTP " + response.statusCode());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Webhook delivery was interrupted.", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Webhook delivery failed.", exception);
    }
  }
}
