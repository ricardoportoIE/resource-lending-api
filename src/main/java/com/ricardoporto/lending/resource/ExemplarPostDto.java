package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExemplarPostDto(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O tipo é obrigatório")
        @Pattern(
            regexp = "LIVRO|ARTIGO|PERIODICO",
            message = "O tipo deve ser LIVRO, ARTIGO ou PERIODICO")
        String tipo,

    // Campos específicos para Livro
    String autor,
    String editora,
    Integer edicao) {}
