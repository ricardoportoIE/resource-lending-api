package com.ricardoporto.lending.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ricardoporto.lending.customer.BaseAPIIntegracaoTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class ResourceCatalogueIntegrationTest extends BaseAPIIntegracaoTest {

  @Test
  void managesMultipleItemsAndFiltersAvailability() {
    var suffix = UUID.randomUUID().toString();
    var resource =
        new CreateResourceRequest(
            "Engineering Laptop", "Developer workstation", ResourceType.LAPTOP, "IT", suffix, true);
    var created = post("/api/v1/resources", resource, ResourceResponse.class);
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertNotNull(created.getBody());

    var first =
        post(
            "/api/v1/resources/" + created.getBody().id() + "/items",
            new CreateResourceItemRequest("ASSET-" + suffix + "-1"),
            ResourceItemResponse.class);
    var second =
        post(
            "/api/v1/resources/" + created.getBody().id() + "/items",
            new CreateResourceItemRequest("ASSET-" + suffix + "-2"),
            ResourceItemResponse.class);
    assertEquals(HttpStatus.CREATED, first.getStatusCode());
    assertEquals(HttpStatus.CREATED, second.getStatusCode());
    assertNotNull(first.getBody());
    assertNotNull(second.getBody());
    assertNotEquals(first.getBody().id(), second.getBody().id());

    var statusResponse =
        patch(
            "/api/v1/resource-items/" + first.getBody().id() + "/status",
            new UpdateResourceItemStatusRequest(ResourceItemStatus.MAINTENANCE),
            ResourceItemResponse.class);
    assertEquals(ResourceItemStatus.MAINTENANCE, statusResponse.getBody().status());

    var filtered =
        rest.exchange(
            "/api/v1/resources?type=LAPTOP&status=AVAILABLE&category=IT",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders()),
            String.class);
    assertEquals(HttpStatus.OK, filtered.getStatusCode());
    assertTrue(filtered.getBody().contains(suffix));

    var detail = get("/api/v1/resources/" + created.getBody().id(), ResourceResponse.class);
    assertEquals(2, detail.getBody().items().size());
  }

  @Test
  void rejectsInvalidResourceType() {
    var response =
        rest.exchange(
            "/api/v1/resources",
            HttpMethod.POST,
            new HttpEntity<>(
                """
                {"name":"Invalid","type":"SPACESHIP","loanable":true}
                """,
                getHeaders()),
            String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("MALFORMED_REQUEST"));
  }
}
