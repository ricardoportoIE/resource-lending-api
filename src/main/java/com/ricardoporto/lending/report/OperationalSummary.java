package com.ricardoporto.lending.report;

import java.time.Instant;

public record OperationalSummary(
    long activeLoans,
    long overdueLoans,
    long returnedLoans,
    long unavailableItems,
    long maintenanceItems,
    double averageReservationWaitSeconds,
    long reservationWaitSampleSize,
    Instant generatedAt) {}
