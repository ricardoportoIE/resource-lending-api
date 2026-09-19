package com.ricardoporto.lending.loan;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
    UUID id,
    Long borrowerId,
    String borrowerEmail,
    UUID resourceItemId,
    String assetTag,
    LoanStatus status,
    Instant requestedAt,
    Instant approvedAt,
    Instant borrowedAt,
    Instant dueAt,
    Instant returnedAt,
    Long approverId,
    long version) {}
