package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.emprestimo;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente.Cliente;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.Exemplar;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Emprestimo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate dataEmprestimo;
  private LocalDate dataDevolucao;

  // Muitos empréstimos para um cliente
  @ManyToOne
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  // Muitos empréstimos para um exemplar
  @ManyToOne
  @JoinColumn(name = "exemplar_id")
  private Exemplar exemplar;
}
