package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
