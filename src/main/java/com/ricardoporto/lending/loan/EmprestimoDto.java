package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.customer.ClienteDto;
import com.ricardoporto.lending.resource.ExemplarDto;
import java.io.Serializable;
import java.time.LocalDate;

public record EmprestimoDto(
    Long id,
    LocalDate dataEmprestimo,
    LocalDate dataDevolucao,
    ClienteDto cliente,
    ExemplarDto exemplar)
    implements Serializable {}
