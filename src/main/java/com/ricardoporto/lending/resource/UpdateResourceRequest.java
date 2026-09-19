package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
    @Size(min = 1, max = 200) String name,
    @Size(max = 2000) String description,
    ResourceType type,
    @Size(max = 100) String category,
    @Size(max = 100) String identifier,
    Boolean loanable) {}
