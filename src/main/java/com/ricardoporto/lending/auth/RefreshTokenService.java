package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.user.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final RefreshTokenRepository repository;
  private final TokenService tokenService;
  private final Duration refreshTtl;

  public RefreshTokenService(
      RefreshTokenRepository repository,
      TokenService tokenService,
      @Value("${api.security.token.refresh-ttl}") Duration refreshTtl) {
    this.repository = repository;
    this.tokenService = tokenService;
    this.refreshTtl = refreshTtl;
  }

  @Transactional
  public AuthTokensDto issue(Usuario usuario) {
    return tokenPair(usuario, create(usuario));
  }

  @Transactional(noRollbackFor = ApiException.class)
  public AuthTokensDto rotate(String rawToken) {
    var current = requireToken(rawToken);
    var now = Instant.now();
    if (current.getRevokedAt() != null) {
      repository.revokeAllActiveByUserId(current.getUsuario().getId(), now);
      throw unauthorized("REFRESH_TOKEN_REUSED", "Refresh token reuse was detected.");
    }
    if (!current.getExpiresAt().isAfter(now)) {
      current.setRevokedAt(now);
      throw unauthorized("REFRESH_TOKEN_EXPIRED", "The refresh token has expired.");
    }

    var replacement = create(current.getUsuario());
    current.setRevokedAt(now);
    current.setReplacedByTokenHash(hash(replacement.rawToken()));
    return tokenPair(current.getUsuario(), replacement);
  }

  @Transactional
  public void revoke(String rawToken) {
    repository
        .findByTokenHashForUpdate(hash(rawToken))
        .ifPresent(
            token -> {
              if (token.getRevokedAt() == null) token.setRevokedAt(Instant.now());
            });
  }

  private IssuedRefreshToken create(Usuario usuario) {
    var bytes = new byte[48];
    SECURE_RANDOM.nextBytes(bytes);
    var rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var now = Instant.now();
    var entity = new RefreshToken();
    entity.setId(UUID.randomUUID());
    entity.setTokenHash(hash(rawToken));
    entity.setUsuario(usuario);
    entity.setCreatedAt(now);
    entity.setExpiresAt(now.plus(refreshTtl));
    repository.save(entity);
    return new IssuedRefreshToken(rawToken);
  }

  private RefreshToken requireToken(String rawToken) {
    return repository
        .findByTokenHashForUpdate(hash(rawToken))
        .orElseThrow(() -> unauthorized("INVALID_REFRESH_TOKEN", "The refresh token is invalid."));
  }

  private AuthTokensDto tokenPair(Usuario usuario, IssuedRefreshToken refreshToken) {
    return new AuthTokensDto(
        tokenService.generateAccessToken(usuario),
        refreshToken.rawToken(),
        "Bearer",
        tokenService.accessTokenTtl().toSeconds());
  }

  private String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  private ApiException unauthorized(String code, String message) {
    return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
  }

  private record IssuedRefreshToken(String rawToken) {}
}
