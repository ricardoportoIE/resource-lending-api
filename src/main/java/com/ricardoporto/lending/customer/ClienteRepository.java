package com.ricardoporto.lending.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  List<Cliente> findByNomeContainingIgnoreCase(String nome);

  List<Cliente> findAllByUsuarioEmail(String email);

  List<Cliente> findByNomeContainingIgnoreCaseAndUsuarioEmail(String nome, String email);
}
