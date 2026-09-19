package com.ricardoporto.lending.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
  Optional<Perfil> findByNome(String nome);
}
