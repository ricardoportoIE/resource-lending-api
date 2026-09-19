package com.ricardoporto.lending.outbox;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/outbox-events")
@Tag(name = "Outbox administration")
@PreAuthorize("hasRole('ADMIN')")
public class OutboxAdminController {
  private final OutboxEventRepository repository;

  public OutboxAdminController(OutboxEventRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  @Operation(summary = "Inspect outbox events, including exhausted deliveries")
  public Page<OutboxEventResponse> findAll(
      @RequestParam(defaultValue = "FAILED") OutboxStatus status, Pageable pageable) {
    return repository
        .findAllByStatusOrderByCreatedAtDesc(status, pageable)
        .map(OutboxEventResponse::from);
  }
}
