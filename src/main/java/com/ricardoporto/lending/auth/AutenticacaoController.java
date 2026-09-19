package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.identity.IdentityService;
import com.ricardoporto.lending.identity.PasswordForgotRequest;
import com.ricardoporto.lending.identity.PasswordResetRequest;
import com.ricardoporto.lending.identity.TokenRequest;
import com.ricardoporto.lending.shared.security.SecurityFilter;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.user.RegistrationRequest;
import com.ricardoporto.lending.user.UserResponse;
import com.ricardoporto.lending.user.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Account registration and token lifecycle")
public class AutenticacaoController {
  private final LoginService loginService;
  private final RefreshTokenService refreshTokenService;
  private final UsuarioService usuarioService;
  private final IdentityService identityService;
  private final TokenService tokenService;

  public AutenticacaoController(
      LoginService loginService,
      RefreshTokenService refreshTokenService,
      UsuarioService usuarioService,
      IdentityService identityService,
      TokenService tokenService) {
    this.loginService = loginService;
    this.refreshTokenService = refreshTokenService;
    this.usuarioService = usuarioService;
    this.identityService = identityService;
    this.tokenService = tokenService;
  }

  @PostMapping("/register")
  @SecurityRequirements
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
    return ResponseEntity.status(201).body(usuarioService.register(request));
  }

  @PostMapping("/login")
  @SecurityRequirements
  public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(loginService.login(request));
  }

  @PostMapping("/refresh")
  @SecurityRequirements
  public ResponseEntity<AuthTokensResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(refreshTokenService.rotate(request.refreshToken()));
  }

  @PostMapping("/logout")
  @SecurityRequirements
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    refreshTokenService.revoke(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/revoke")
  public ResponseEntity<Void> revokeAccessToken(HttpServletRequest request) {
    tokenService.revoke(SecurityFilter.bearerTokenValue(request));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/email/confirm")
  @SecurityRequirements
  public ResponseEntity<Void> confirmEmail(@Valid @RequestBody TokenRequest request) {
    identityService.confirmEmail(request.token());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/email/resend")
  @SecurityRequirements
  public ResponseEntity<Void> resendEmail(@Valid @RequestBody PasswordForgotRequest request) {
    identityService.resendEmailConfirmation(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/password/forgot")
  @SecurityRequirements
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody PasswordForgotRequest request) {
    identityService.requestPasswordReset(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/password/reset")
  @SecurityRequirements
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
    identityService.resetPassword(request);
    return ResponseEntity.noContent().build();
  }
}
