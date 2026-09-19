package com.ricardoporto.lending.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioAutenticacaoDTO(
    @NotBlank(message = "O email é obrigatório") @Email(message = "O email deve ser válido")
        String email,
    @NotBlank(message = "A senha é obrigatória") String senha) {
  @Override
  public String toString() {
    return "UsuarioAutenticacaoDTO[email=" + email + ", senha=<redacted>]";
  }
}
