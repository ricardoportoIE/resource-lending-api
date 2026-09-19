package com.ricardoporto.lending.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroDto(
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String senha) {
  @Override
  public String toString() {
    return "UsuarioCadastroDto[email=" + email + ", senha=<redacted>]";
  }
}
