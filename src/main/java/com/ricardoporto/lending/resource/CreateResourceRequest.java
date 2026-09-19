package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @NotNull ResourceType type,
    @Size(max = 100) String category,
    @Size(max = 100) String identifier,
    boolean loanable) {}
