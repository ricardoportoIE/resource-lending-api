package com.ricardoporto.lending.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ApiErrorContractIntegrationTest extends BaseAPIIntegracaoTest {

  @Test
  void shouldReturnProblemDetailWhenResourceDoesNotExist() {
    var response = get("/api/v1/clientes/999999999", JsonNode.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getHeaders().getContentType());
    assertTrue(
        MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
            response.getHeaders().getContentType()));
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().path("status").asInt());
    assertEquals("RESOURCE_NOT_FOUND", response.getBody().path("code").asText());
    assertEquals("/api/v1/clientes/999999999", response.getBody().path("instance").asText());
    assertTrue(response.getBody().hasNonNull("timestamp"));
  }

  @Test
  void shouldReturnFieldErrorsForInvalidRequest() {
    var invalidRequest = new ClientePostDto("", 0, "invalid", "", "INVALID");

    var response = post("/api/v1/clientes", invalidRequest, JsonNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("VALIDATION_ERROR", response.getBody().path("code").asText());
    assertTrue(response.getBody().path("errors").has("nome"));
    assertTrue(response.getBody().path("errors").has("idade"));
    assertTrue(response.getBody().path("errors").has("telefone"));
    assertTrue(response.getBody().path("errors").has("tipo"));
  }
}
