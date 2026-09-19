package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.user.Usuario;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;

  public LoginService(
      AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
    this.authenticationManager = authenticationManager;
    this.refreshTokenService = refreshTokenService;
  }

  public AuthTokensDto login(UsuarioAutenticacaoDTO request) {
    var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    var credentials = new UsernamePasswordAuthenticationToken(normalizedEmail, request.senha());
    var authentication = authenticationManager.authenticate(credentials);
    return refreshTokenService.issue((Usuario) authentication.getPrincipal());
  }
}
