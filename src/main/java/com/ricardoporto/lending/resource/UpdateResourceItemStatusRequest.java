package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.NotNull;

public record UpdateResourceItemStatusRequest(@NotNull ResourceItemStatus status) {}
