package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.emprestimo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PastOrPresent;
import java.io.Serializable;
import java.time.LocalDate;

public record EmprestimoPatchDto(
    @PastOrPresent(message = "A data de empréstimo deve ser no passado ou presente")
        LocalDate dataEmprestimo,
    @FutureOrPresent(message = "A data de devolução deve ser no futuro ou presente")
        LocalDate dataDevolucao,
    Long clienteId,
    Long exemplarId)
    implements Serializable {}
