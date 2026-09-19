package com.ricardoporto.lending.resource;

import java.time.Instant;
import java.util.UUID;

public record ResourceItemResponse(
    UUID id, String assetTag, ResourceItemStatus status, long version, Instant updatedAt) {}
