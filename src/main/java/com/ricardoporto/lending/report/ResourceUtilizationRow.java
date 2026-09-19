package com.ricardoporto.lending.report;

import java.util.UUID;

public record ResourceUtilizationRow(
    UUID resourceId,
    String resourceName,
    long totalItems,
    long onLoanItems,
    long unavailableItems,
    double currentUtilizationPercent) {}
