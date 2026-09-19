package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResourceItemRequest(@NotBlank @Size(max = 100) String assetTag) {}
