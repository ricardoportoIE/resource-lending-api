package com.ricardoporto.lending.customer;

public final class ClienteMapper {

  private ClienteMapper() {}

  public static ClienteDto toDto(Cliente cliente) {
    return new ClienteDto(
        cliente.getCodigo(),
        cliente.getNome(),
        cliente.getIdade(),
        cliente.getTelefone(),
        cliente.getEndereco(),
        cliente.getClass().getSimpleName());
  }
}
