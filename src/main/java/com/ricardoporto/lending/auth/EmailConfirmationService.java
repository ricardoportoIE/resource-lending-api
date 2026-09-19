package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.shared.exception.TokenInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailConfirmationService {

  private final String confirmationToken;

  public EmailConfirmationService(
      @Value("${api.security.email-confirmation-token}") String confirmationToken) {
    this.confirmationToken = confirmationToken;
  }

  public void confirm(String token) {
    if (!confirmationToken.equals(token)) {
      throw new TokenInvalidoException("The email confirmation token is invalid.");
    }
  }
}
