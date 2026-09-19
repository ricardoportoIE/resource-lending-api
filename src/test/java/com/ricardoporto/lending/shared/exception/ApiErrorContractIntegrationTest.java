package com.ricardoporto.lending.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricardoporto.lending.support.BaseApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ApiErrorContractIntegrationTest extends BaseApiIntegrationTest {

  @Test
  void returnsProblemDetailWhenResourceDoesNotExist() {
    var path = "/api/v1/resources/" + UUID.randomUUID();
    var response = get(path, JsonNode.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getHeaders().getContentType());
    assertTrue(
        MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
            response.getHeaders().getContentType()));
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().path("status").asInt());
    assertEquals("RESOURCE_NOT_FOUND", response.getBody().path("code").asText());
    assertEquals(path, response.getBody().path("instance").asText());
    assertTrue(response.getBody().hasNonNull("timestamp"));
    assertTrue(response.getBody().hasNonNull("correlationId"));
  }

  @Test
  void returnsFieldErrorsForInvalidRequest() {
    var invalidRequest =
        """
        {"name":"","type":null,"loanable":true}
        """;
    var response = post("/api/v1/resources", invalidRequest, JsonNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("VALIDATION_ERROR", response.getBody().path("code").asText());
    assertTrue(response.getBody().path("errors").has("name"));
    assertTrue(response.getBody().path("errors").has("type"));
  }
}
