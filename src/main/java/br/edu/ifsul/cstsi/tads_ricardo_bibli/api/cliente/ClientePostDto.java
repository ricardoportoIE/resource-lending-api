package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ClientePostDto(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotNull(message = "A idade é obrigatória")
        @Min(value = 1, message = "A idade deve ser maior que zero")
        Integer idade,
    @NotBlank(message = "O telefone é obrigatório")
        @Pattern(
            regexp = "^[0-9]{10,11}$",
            message = "O telefone deve conter entre 10 e 11 dígitos numéricos")
        String telefone,
    @NotBlank(message = "O endereço é obrigatório") String endereco,
    @NotBlank(message = "O tipo é obrigatório")
        @Pattern(regexp = "ALUNO|PAI_DE_ALUNO", message = "O tipo deve ser ALUNO ou PAI_DE_ALUNO")
        String tipo) {}
