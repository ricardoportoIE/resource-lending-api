package com.ricardoporto.lending.customer;

import java.io.Serializable;

public record ClienteDto(
    Long codigo,
    String nome,
    Integer idade,
    String telefone,
    String endereco,
    String tipo,
    Long ownerUserId)
    implements Serializable {
  public ClienteDto(
      Long codigo, String nome, Integer idade, String telefone, String endereco, String tipo) {
    this(codigo, nome, idade, telefone, endereco, tipo, null);
  }
}
