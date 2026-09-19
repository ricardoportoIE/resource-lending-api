package com.ricardoporto.lending.identity;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {}
