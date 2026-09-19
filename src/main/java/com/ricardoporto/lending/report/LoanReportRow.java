package com.ricardoporto.lending.report;

import com.ricardoporto.lending.loan.LoanStatus;
import java.time.Instant;
import java.util.UUID;

public record LoanReportRow(
    UUID loanId,
    LoanStatus status,
    String borrowerEmail,
    String resourceName,
    String assetTag,
    Instant requestedAt,
    Instant dueAt,
    Instant returnedAt) {}
