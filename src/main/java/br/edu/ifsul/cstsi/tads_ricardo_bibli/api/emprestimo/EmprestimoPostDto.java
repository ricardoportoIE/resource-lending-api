package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.emprestimo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record EmprestimoPostDto(
    @NotNull(message = "A data de empréstimo é obrigatória")
        @PastOrPresent(message = "A data de empréstimo deve ser no passado ou presente")
        LocalDate dataEmprestimo,
    @FutureOrPresent(message = "A data de devolução deve ser no futuro ou presente")
        LocalDate dataDevolucao,
    @NotNull(message = "O cliente é obrigatório") Long clienteId,
    @NotNull(message = "O exemplar é obrigatório") Long exemplarId) {}
