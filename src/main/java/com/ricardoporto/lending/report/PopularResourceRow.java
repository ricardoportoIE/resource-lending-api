package com.ricardoporto.lending.report;

import java.util.UUID;

public record PopularResourceRow(
    UUID resourceId,
    String resourceName,
    long loanRequests,
    long reservationRequests,
    long totalRequests) {}
