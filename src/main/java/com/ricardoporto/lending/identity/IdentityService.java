package com.ricardoporto.lending.identity;

import com.ricardoporto.lending.auth.RefreshTokenRepository;
import com.ricardoporto.lending.outbox.OutboxService;
import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.user.Usuario;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final IdentityTokenRepository tokens;
  private final UsuarioRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final OutboxService outbox;
  private final Duration confirmationTtl;
  private final Duration resetTtl;

  public IdentityService(
      IdentityTokenRepository tokens,
      UsuarioRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      OutboxService outbox,
      @Value("${api.security.identity.confirmation-ttl}") Duration confirmationTtl,
      @Value("${api.security.identity.password-reset-ttl}") Duration resetTtl) {
    this.tokens = tokens;
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.outbox = outbox;
    this.confirmationTtl = confirmationTtl;
    this.resetTtl = resetTtl;
  }

  @Transactional
  public void sendEmailConfirmation(Usuario user) {
    if (user.isConfirmado()) return;
    issue(user, IdentityTokenPurpose.EMAIL_CONFIRMATION, confirmationTtl);
  }

  @Transactional
  public void resendEmailConfirmation(String email) {
    users
        .findOptionalByEmail(normalize(email))
        .filter(user -> !user.isConfirmado())
        .ifPresent(this::sendEmailConfirmation);
  }

  @Transactional
  public void confirmEmail(String rawToken) {
    var token = requireUsable(rawToken, IdentityTokenPurpose.EMAIL_CONFIRMATION);
    token.getUsuario().setConfirmado(true);
    token.setConsumedAt(Instant.now());
  }

  @Transactional
  public void requestPasswordReset(String email) {
    users
        .findOptionalByEmail(normalize(email))
        .filter(Usuario::isConfirmado)
        .ifPresent(user -> issue(user, IdentityTokenPurpose.PASSWORD_RESET, resetTtl));
  }

  @Transactional
  public void resetPassword(PasswordResetRequest request) {
    var token = requireUsable(request.token(), IdentityTokenPurpose.PASSWORD_RESET);
    var user = token.getUsuario();
    user.setSenha(passwordEncoder.encode(request.newPassword()));
    user.setSecurityVersion(user.getSecurityVersion() + 1);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    token.setConsumedAt(Instant.now());
    refreshTokens.revokeAllActiveByUserId(user.getId(), Instant.now());
  }

  private void issue(Usuario user, IdentityTokenPurpose purpose, Duration ttl) {
    var bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    var rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var now = Instant.now();
    var token = new IdentityToken();
    token.setId(UUID.randomUUID());
    token.setTokenHash(hash(rawToken));
    token.setUsuario(user);
    token.setPurpose(purpose);
    token.setCreatedAt(now);
    token.setExpiresAt(now.plus(ttl));
    tokens.save(token);

    var eventType =
        purpose == IdentityTokenPurpose.EMAIL_CONFIRMATION
            ? "EMAIL_CONFIRMATION_REQUESTED"
            : "PASSWORD_RESET_REQUESTED";
    var actionPath =
        purpose == IdentityTokenPurpose.EMAIL_CONFIRMATION
            ? "/api/v1/auth/email/confirm"
            : "/api/v1/auth/password/reset";
    outbox.publish(
        eventType,
        "USER",
        user.getId(),
        Map.of("recipient", user.getEmail(), "token", rawToken, "actionPath", actionPath),
        eventType + ":" + token.getId());
  }

  private IdentityToken requireUsable(String rawToken, IdentityTokenPurpose purpose) {
    var token =
        tokens.findForUpdate(hash(rawToken), purpose).orElseThrow(() -> invalidToken(purpose));
    if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) {
      throw invalidToken(purpose);
    }
    return token;
  }

  private ApiException invalidToken(IdentityTokenPurpose purpose) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "INVALID_IDENTITY_TOKEN",
        "The " + purpose.name().toLowerCase(Locale.ROOT).replace('_', ' ') + " token is invalid.");
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
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
}
