package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.livro;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.Exemplar;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Livro extends Exemplar {

  private String autor;
  private String editora;
  private Integer edicao;
}
