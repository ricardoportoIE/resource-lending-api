package com.ricardoporto.lending.resource;

public final class ExemplarMapper {

  private ExemplarMapper() {}

  public static ExemplarDto toDto(Exemplar exemplar) {
    return new ExemplarDto(
        exemplar.getCodigo(), exemplar.getNome(), exemplar.getClass().getSimpleName());
  }
}
