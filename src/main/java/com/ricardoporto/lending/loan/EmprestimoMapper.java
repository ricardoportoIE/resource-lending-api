package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.customer.ClienteMapper;
import com.ricardoporto.lending.resource.ExemplarMapper;

final class EmprestimoMapper {

  private EmprestimoMapper() {}

  static EmprestimoDto toDto(Emprestimo emprestimo) {
    return new EmprestimoDto(
        emprestimo.getId(),
        emprestimo.getDataEmprestimo(),
        emprestimo.getDataDevolucao(),
        emprestimo.getCliente() == null ? null : ClienteMapper.toDto(emprestimo.getCliente()),
        emprestimo.getExemplar() == null ? null : ExemplarMapper.toDto(emprestimo.getExemplar()));
  }
}
