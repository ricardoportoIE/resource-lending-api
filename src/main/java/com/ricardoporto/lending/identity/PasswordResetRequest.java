package com.ricardoporto.lending.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank String token, @NotBlank @Size(min = 12, max = 128) String newPassword) {}
