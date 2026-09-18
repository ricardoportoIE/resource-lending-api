package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioAutenticacaoDTO(
    @NotBlank(message = "O email é obrigatório") @Email(message = "O email deve ser válido")
        String email,
    @NotBlank(message = "A senha é obrigatória") String senha) {}
