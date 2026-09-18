package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar;

import java.io.Serializable;

public record ExemplarDto(Long codigo, String nome, String tipo) implements Serializable {
  public ExemplarDto(Exemplar exemplar) {
    this(exemplar.getCodigo(), exemplar.getNome(), exemplar.getClass().getSimpleName());
  }
}
