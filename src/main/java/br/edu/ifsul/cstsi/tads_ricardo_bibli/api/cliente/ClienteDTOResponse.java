package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente;

/** DTO for {@link Cliente} */
public record ClienteDTOResponse(
    Long codigo, String nome, Integer idade, String telefone, String endereco) {
  public ClienteDTOResponse(Cliente cliente) {
    this(
        cliente.getCodigo(),
        cliente.getNome(),
        cliente.getIdade(),
        cliente.getTelefone(),
        cliente.getEndereco());
  }
}
