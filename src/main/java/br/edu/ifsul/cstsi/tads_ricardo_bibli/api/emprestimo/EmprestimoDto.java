package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.emprestimo;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente.ClienteDto;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.ExemplarDto;
import java.io.Serializable;
import java.time.LocalDate;

public record EmprestimoDto(
    Long id,
    LocalDate dataEmprestimo,
    LocalDate dataDevolucao,
    ClienteDto cliente,
    ExemplarDto exemplar)
    implements Serializable {
  public EmprestimoDto(Emprestimo emprestimo) {
    this(
        emprestimo.getId(),
        emprestimo.getDataEmprestimo(),
        emprestimo.getDataDevolucao(),
        emprestimo.getCliente() != null ? new ClienteDto(emprestimo.getCliente()) : null,
        emprestimo.getExemplar() != null ? new ExemplarDto(emprestimo.getExemplar()) : null);
  }
}
