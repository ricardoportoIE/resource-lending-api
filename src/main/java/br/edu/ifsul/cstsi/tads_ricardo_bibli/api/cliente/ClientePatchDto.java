package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ClientePatchDto(
    @Size(min = 3, message = "O nome deve ter pelo menos 3 caracteres") String nome,
    @Min(value = 1, message = "A idade deve ser maior que zero") Integer idade,
    @Pattern(
            regexp = "^[0-9]{10,11}$",
            message = "O telefone deve conter entre 10 e 11 dígitos numéricos")
        String telefone,
    @Size(min = 5, message = "O endereço deve ter pelo menos 5 caracteres") String endereco)
    implements Serializable {}
