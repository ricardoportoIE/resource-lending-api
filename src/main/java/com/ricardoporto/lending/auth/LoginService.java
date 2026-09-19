package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.user.Usuario;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;
  private final LoginAttemptService loginAttempts;

  public LoginService(
      AuthenticationManager authenticationManager,
      RefreshTokenService refreshTokenService,
      LoginAttemptService loginAttempts) {
    this.authenticationManager = authenticationManager;
    this.refreshTokenService = refreshTokenService;
    this.loginAttempts = loginAttempts;
  }

  public AuthTokensResponse login(LoginRequest request) {
    var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    loginAttempts.assertNotLocked(normalizedEmail);
    var credentials = new UsernamePasswordAuthenticationToken(normalizedEmail, request.password());
    try {
      var authentication = authenticationManager.authenticate(credentials);
      loginAttempts.recordSuccess(normalizedEmail);
      return refreshTokenService.issue((Usuario) authentication.getPrincipal());
    } catch (AuthenticationException exception) {
      loginAttempts.recordFailure(normalizedEmail);
      throw new BadCredentialsException("Invalid credentials.", exception);
    }
  }
}
