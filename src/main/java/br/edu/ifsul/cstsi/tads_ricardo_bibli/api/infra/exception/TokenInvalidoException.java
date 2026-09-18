package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.infra.exception;

public class TokenInvalidoException extends RuntimeException {
  public TokenInvalidoException(String mensagem) {
    super(mensagem);
  }
}
