package com.ricardoporto.lending.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ricardoporto.lending.shared.exception.TokenInvalidoException;
import com.ricardoporto.lending.user.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private static final String ISSUER = "resource-lending-api";
  private final String secret;
  private final Duration accessTtl;

  public TokenService(
      @Value("${api.security.token.secret}") String secret,
      @Value("${api.security.token.access-ttl}") Duration accessTtl) {
    this.secret = secret;
    this.accessTtl = accessTtl;
  }

  public String generateAccessToken(Usuario usuario) {
    try {
      return JWT.create()
          .withIssuer(ISSUER)
          .withSubject(usuario.getUsername())
          .withIssuedAt(Instant.now())
          .withExpiresAt(Instant.now().plus(accessTtl))
          .sign(Algorithm.HMAC256(secret));
    } catch (JWTCreationException exception) {
      throw new IllegalStateException("Access token could not be generated.", exception);
    }
  }

  public String getSubject(String token) {
    try {
      return JWT.require(Algorithm.HMAC256(secret))
          .withIssuer(ISSUER)
          .build()
          .verify(token)
          .getSubject();
    } catch (JWTVerificationException exception) {
      throw new TokenInvalidoException("The access token is invalid or expired.");
    }
  }

  public Duration accessTokenTtl() {
    return accessTtl;
  }
}
