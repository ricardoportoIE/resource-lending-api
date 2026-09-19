package com.ricardoporto.lending.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {
  List<Emprestimo> findAllByClienteUsuarioEmail(String email);
}
