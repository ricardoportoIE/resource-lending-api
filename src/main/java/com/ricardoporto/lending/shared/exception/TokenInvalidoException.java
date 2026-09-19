package com.ricardoporto.lending.shared.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidoException extends ApiException {
  public TokenInvalidoException(String message) {
    super(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", message);
  }
}
