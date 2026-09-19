package com.ricardoporto.lending.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfigurableNotificationDeliveryTest {

  @Test
  void usesStableEventIdAsRecipientIdempotencyKeyAcrossRetries() throws Exception {
    var receivedKeys = new ArrayList<String>();
    var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/notifications",
        exchange -> {
          receivedKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();

    try {
      var event = new OutboxEvent();
      event.setId(UUID.randomUUID());
      event.setPayload("{\"message\":\"delivered\"}");
      var delivery =
          new ConfigurableNotificationDelivery(
              "http://localhost:" + server.getAddress().getPort() + "/notifications");

      delivery.deliver(event);
      delivery.deliver(event);

      assertEquals(2, receivedKeys.size());
      assertEquals(event.getId().toString(), receivedKeys.get(0));
      assertEquals(receivedKeys.get(0), receivedKeys.get(1));
    } finally {
      server.stop(0);
    }
  }
}
