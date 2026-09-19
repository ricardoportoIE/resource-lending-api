package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
  private final UsuarioRepository users;
  private final int threshold;
  private final Duration baseLock;
  private final Duration maxLock;

  public LoginAttemptService(
      UsuarioRepository users,
      @Value("${api.security.login-lock.threshold}") int threshold,
      @Value("${api.security.login-lock.base-duration}") Duration baseLock,
      @Value("${api.security.login-lock.max-duration}") Duration maxLock) {
    this.users = users;
    this.threshold = threshold;
    this.baseLock = baseLock;
    this.maxLock = maxLock;
  }

  @Transactional(readOnly = true)
  public void assertNotLocked(String email) {
    users
        .findOptionalByEmail(normalize(email))
        .filter(user -> !user.isAccountNonLocked())
        .ifPresent(
            ignored -> {
              throw new ApiException(
                  HttpStatus.TOO_MANY_REQUESTS,
                  "ACCOUNT_TEMPORARILY_LOCKED",
                  "The account is temporarily locked after repeated login failures.");
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(String email) {
    users
        .findByEmailForUpdate(normalize(email))
        .ifPresent(
            user -> {
              var failures = user.getFailedLoginAttempts() + 1;
              user.setFailedLoginAttempts(failures);
              if (failures >= threshold) {
                var exponent = Math.min(failures - threshold, 20);
                var seconds =
                    Math.min(maxLock.toSeconds(), baseLock.toSeconds() * (1L << exponent));
                user.setLockedUntil(Instant.now().plusSeconds(seconds));
              }
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordSuccess(String email) {
    users
        .findByEmailForUpdate(normalize(email))
        .ifPresent(
            user -> {
              user.setFailedLoginAttempts(0);
              user.setLockedUntil(null);
            });
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
