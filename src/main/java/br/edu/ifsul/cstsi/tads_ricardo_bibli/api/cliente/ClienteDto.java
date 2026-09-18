package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente;

import java.io.Serializable;

public record ClienteDto(
    Long codigo, String nome, Integer idade, String telefone, String endereco, String tipo)
    implements Serializable {
  public ClienteDto(Cliente cliente) {
    this(
        cliente.getCodigo(),
        cliente.getNome(),
        cliente.getIdade(),
        cliente.getTelefone(),
        cliente.getEndereco(),
        cliente.getClass().getSimpleName());
  }
}
