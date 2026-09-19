package com.ricardoporto.lending.loan;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Policy-driven lending state machine")
public class LoanController {
  private final LoanWorkflowService loanWorkflowService;

  public LoanController(LoanWorkflowService loanWorkflowService) {
    this.loanWorkflowService = loanWorkflowService;
  }

  @PostMapping
  public ResponseEntity<LoanResponse> request(
      @Valid @RequestBody CreateLoanRequest request,
      @Parameter(
              name = "Idempotency-Key",
              in = ParameterIn.HEADER,
              description = "Optional retry key; identical requests replay the original response")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idempotencyKey) {
    var created = loanWorkflowService.request(request);
    return ResponseEntity.created(URI.create("/api/v1/loans/" + created.id())).body(created);
  }

  @GetMapping
  public ResponseEntity<List<LoanResponse>> findAll() {
    return ResponseEntity.ok(loanWorkflowService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<LoanResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.findById(id));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<LoanResponse> approve(
      @PathVariable UUID id,
      @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER)
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idempotencyKey) {
    return ResponseEntity.ok(loanWorkflowService.approve(id));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<LoanResponse> reject(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.reject(id));
  }

  @PostMapping("/{id}/collect")
  public ResponseEntity<LoanResponse> collect(
      @PathVariable UUID id,
      @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER)
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idempotencyKey) {
    return ResponseEntity.ok(loanWorkflowService.collect(id));
  }

  @PostMapping("/{id}/return")
  public ResponseEntity<LoanResponse> returnLoan(
      @PathVariable UUID id,
      @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER)
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idempotencyKey) {
    return ResponseEntity.ok(loanWorkflowService.returnLoan(id));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<LoanResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.cancel(id));
  }
}
