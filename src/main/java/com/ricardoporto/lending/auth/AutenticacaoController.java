package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.user.RegistrationRequest;
import com.ricardoporto.lending.user.UserResponse;
import com.ricardoporto.lending.user.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
@Tag(name = "Authentication", description = "Account registration and token lifecycle")
public class AutenticacaoController {
  private final LoginService loginService;
  private final RefreshTokenService refreshTokenService;
  private final UsuarioService usuarioService;

  public AutenticacaoController(
      LoginService loginService,
      RefreshTokenService refreshTokenService,
      UsuarioService usuarioService) {
    this.loginService = loginService;
    this.refreshTokenService = refreshTokenService;
    this.usuarioService = usuarioService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
    return ResponseEntity.status(201).body(usuarioService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(loginService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthTokensResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(refreshTokenService.rotate(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    refreshTokenService.revoke(request.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
