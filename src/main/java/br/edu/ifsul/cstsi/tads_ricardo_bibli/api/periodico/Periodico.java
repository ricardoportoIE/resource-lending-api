package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.periodico;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.Exemplar;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Periodico extends Exemplar {

  private String editora;
}
