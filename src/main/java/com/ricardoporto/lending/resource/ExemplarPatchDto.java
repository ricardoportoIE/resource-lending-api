package com.ricardoporto.lending.resource;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ExemplarPatchDto(
    @Size(min = 3, message = "O nome deve ter pelo menos 3 caracteres") String nome,

    // Campos específicos para Livro e Artigo
    String autor,

    // Campos específicos para Livro e Periodico
    String editora,

    // Campo específico para Livro
    Integer edicao)
    implements Serializable {}
