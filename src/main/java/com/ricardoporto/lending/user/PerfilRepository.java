package com.ricardoporto.lending.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
  Perfil findByNome(String nome);
}
