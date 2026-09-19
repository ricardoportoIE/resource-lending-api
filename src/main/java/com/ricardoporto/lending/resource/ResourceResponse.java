package com.ricardoporto.lending.resource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResourceResponse(
    UUID id,
    String name,
    String description,
    ResourceType type,
    String category,
    String identifier,
    boolean loanable,
    Instant createdAt,
    Instant updatedAt,
    List<ResourceItemResponse> items) {}
