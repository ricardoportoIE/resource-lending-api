package com.ricardoporto.lending.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ricardoporto.lending.shared.exception.TokenInvalidoException;
import com.ricardoporto.lending.user.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenServiceTest {
  private static final String CURRENT_SECRET = "current-signing-secret-at-least-32-characters";
  private static final String PREVIOUS_SECRET = "previous-signing-secret-at-least-32-characters";

  @Test
  void issuesWithTheActiveKeyAndAcceptsAConfiguredPreviousKey() {
    var repository = mock(RevokedAccessTokenRepository.class);
    when(repository.existsById("previous-jti")).thenReturn(false);
    var service =
        new TokenService(
            "unused-legacy-secret-at-least-32-characters",
            "current",
            "current:" + CURRENT_SECRET + ",previous:" + PREVIOUS_SECRET,
            Duration.ofMinutes(15),
            repository);
    var user = new Usuario();
    user.setEmail("rotation@example.com");

    assertEquals("current", JWT.decode(service.generateAccessToken(user)).getKeyId());

    var previousToken =
        JWT.create()
            .withIssuer("resource-lending-api")
            .withSubject(user.getEmail())
            .withKeyId("previous")
            .withJWTId("previous-jti")
            .withClaim("ver", 0)
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256(PREVIOUS_SECRET));
    assertEquals(user.getEmail(), service.verify(previousToken).subject());
  }

  @Test
  void rejectsAnUnconfiguredKeyIdentifier() {
    var service =
        new TokenService(
            CURRENT_SECRET,
            "primary",
            "",
            Duration.ofMinutes(15),
            mock(RevokedAccessTokenRepository.class));
    var token =
        JWT.create()
            .withIssuer("resource-lending-api")
            .withSubject("unknown@example.com")
            .withKeyId("retired")
            .withJWTId("unknown-jti")
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256(PREVIOUS_SECRET));

    assertThrows(TokenInvalidoException.class, () -> service.verify(token));
  }
}
