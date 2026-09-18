package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.artigo;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.Exemplar;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Artigo extends Exemplar {

  private String autor;
}
