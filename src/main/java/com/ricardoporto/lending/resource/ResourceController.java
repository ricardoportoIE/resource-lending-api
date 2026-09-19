package com.ricardoporto.lending.resource;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ResourceController {
  private final ResourceCatalogueService resourceCatalogueService;

  public ResourceController(ResourceCatalogueService resourceCatalogueService) {
    this.resourceCatalogueService = resourceCatalogueService;
  }

  @PostMapping("/resources")
  public ResponseEntity<ResourceResponse> create(
      @Valid @RequestBody CreateResourceRequest request) {
    var created = resourceCatalogueService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/resources/" + created.id())).body(created);
  }

  @GetMapping("/resources")
  public ResponseEntity<Page<ResourceResponse>> findAll(
      @RequestParam(required = false) ResourceType type,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) ResourceItemStatus status,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(resourceCatalogueService.findAll(type, category, status, pageable));
  }

  @GetMapping("/resources/{id}")
  public ResponseEntity<ResourceResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(resourceCatalogueService.findById(id));
  }

  @PatchMapping("/resources/{id}")
  public ResponseEntity<ResourceResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateResourceRequest request) {
    return ResponseEntity.ok(resourceCatalogueService.update(id, request));
  }

  @PostMapping("/resources/{id}/items")
  public ResponseEntity<ResourceItemResponse> addItem(
      @PathVariable UUID id, @Valid @RequestBody CreateResourceItemRequest request) {
    var created = resourceCatalogueService.addItem(id, request);
    return ResponseEntity.created(URI.create("/api/v1/resource-items/" + created.id()))
        .body(created);
  }

  @PatchMapping("/resource-items/{id}/status")
  public ResponseEntity<ResourceItemResponse> updateItemStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateResourceItemStatusRequest request) {
    return ResponseEntity.ok(resourceCatalogueService.updateItemStatus(id, request));
  }
}
