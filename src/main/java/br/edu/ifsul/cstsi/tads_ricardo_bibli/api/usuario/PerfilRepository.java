package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
  Perfil findByNome(String nome);
}
