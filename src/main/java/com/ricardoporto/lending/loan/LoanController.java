package com.ricardoporto.lending.loan;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
  private final LoanWorkflowService loanWorkflowService;

  public LoanController(LoanWorkflowService loanWorkflowService) {
    this.loanWorkflowService = loanWorkflowService;
  }

  @PostMapping
  public ResponseEntity<LoanResponse> request(@Valid @RequestBody CreateLoanRequest request) {
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
  public ResponseEntity<LoanResponse> approve(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.approve(id));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<LoanResponse> reject(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.reject(id));
  }

  @PostMapping("/{id}/collect")
  public ResponseEntity<LoanResponse> collect(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.collect(id));
  }

  @PostMapping("/{id}/return")
  public ResponseEntity<LoanResponse> returnLoan(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.returnLoan(id));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<LoanResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(loanWorkflowService.cancel(id));
  }
}
