package com.ricardoporto.lending.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ricardoporto.lending.shared.exception.TokenInvalidoException;
import com.ricardoporto.lending.user.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
  private static final String ISSUER = "resource-lending-api";
  private final Map<String, Algorithm> verificationKeys;
  private final String activeKeyId;
  private final Duration accessTtl;
  private final RevokedAccessTokenRepository revokedTokens;

  public TokenService(
      @Value("${api.security.token.secret}") String legacySecret,
      @Value("${api.security.token.active-key-id:primary}") String activeKeyId,
      @Value("${api.security.token.keys:}") String configuredKeys,
      @Value("${api.security.token.access-ttl}") Duration accessTtl,
      RevokedAccessTokenRepository revokedTokens) {
    this.activeKeyId = activeKeyId;
    this.verificationKeys = parseKeys(configuredKeys, legacySecret);
    if (!verificationKeys.containsKey(activeKeyId)) {
      throw new IllegalStateException(
          "JWT_ACTIVE_KID must identify one of the configured JWT_KEYS.");
    }
    this.accessTtl = accessTtl;
    this.revokedTokens = revokedTokens;
  }

  public String generateAccessToken(Usuario usuario) {
    try {
      return JWT.create()
          .withIssuer(ISSUER)
          .withSubject(usuario.getUsername())
          .withKeyId(activeKeyId)
          .withJWTId(UUID.randomUUID().toString())
          .withClaim("ver", usuario.getSecurityVersion())
          .withIssuedAt(Instant.now())
          .withExpiresAt(Instant.now().plus(accessTtl))
          .sign(verificationKeys.get(activeKeyId));
    } catch (JWTCreationException exception) {
      throw new IllegalStateException("Access token could not be generated.", exception);
    }
  }

  public String getSubject(String token) {
    return verify(token).subject();
  }

  public VerifiedAccessToken verify(String token) {
    try {
      var decoded = JWT.decode(token);
      var keyId = decoded.getKeyId();
      var algorithm = verificationKeys.get(keyId);
      if (algorithm == null) throw new JWTVerificationException("Unknown signing key.");
      var verified = JWT.require(algorithm).withIssuer(ISSUER).build().verify(token);
      if (revokedTokens.existsById(verified.getId())) {
        throw new JWTVerificationException("Access token was revoked.");
      }
      return toVerifiedToken(verified);
    } catch (JWTVerificationException exception) {
      throw new TokenInvalidoException("The access token is invalid or expired.");
    }
  }

  @Transactional
  public void revoke(String rawToken) {
    var verified = verify(rawToken);
    var revoked = new RevokedAccessToken();
    revoked.setJwtId(verified.jwtId());
    revoked.setExpiresAt(verified.expiresAt());
    revoked.setRevokedAt(Instant.now());
    revokedTokens.save(revoked);
  }

  @Scheduled(fixedDelayString = "${api.security.token.revocation-cleanup-ms:3600000}")
  @Transactional
  public void removeExpiredRevocations() {
    revokedTokens.deleteExpired(Instant.now());
  }

  public Duration accessTokenTtl() {
    return accessTtl;
  }

  private VerifiedAccessToken toVerifiedToken(DecodedJWT token) {
    var versionClaim = token.getClaim("ver");
    return new VerifiedAccessToken(
        token.getSubject(),
        token.getId(),
        token.getExpiresAtAsInstant(),
        versionClaim.isNull() ? 0 : versionClaim.asInt());
  }

  private Map<String, Algorithm> parseKeys(String configuredKeys, String legacySecret) {
    var result = new LinkedHashMap<String, Algorithm>();
    if (configuredKeys != null && !configuredKeys.isBlank()) {
      Arrays.stream(configuredKeys.split(","))
          .map(String::trim)
          .filter(entry -> !entry.isEmpty())
          .forEach(
              entry -> {
                var separator = entry.indexOf(':');
                if (separator < 1 || separator == entry.length() - 1) {
                  throw new IllegalStateException("JWT_KEYS must use kid:secret entries.");
                }
                result.put(
                    entry.substring(0, separator),
                    Algorithm.HMAC256(entry.substring(separator + 1)));
              });
    } else {
      result.put("primary", Algorithm.HMAC256(legacySecret));
    }
    return Map.copyOf(result);
  }

  public record VerifiedAccessToken(
      String subject, String jwtId, Instant expiresAt, int securityVersion) {}
}
