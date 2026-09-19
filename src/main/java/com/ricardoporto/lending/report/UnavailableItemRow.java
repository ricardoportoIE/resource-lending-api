package com.ricardoporto.lending.report;

import com.ricardoporto.lending.resource.ResourceItemStatus;
import java.time.Instant;
import java.util.UUID;

public record UnavailableItemRow(
    UUID itemId,
    String assetTag,
    UUID resourceId,
    String resourceName,
    ResourceItemStatus status,
    Instant updatedAt) {}
