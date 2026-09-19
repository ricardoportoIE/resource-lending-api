package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.shared.security.TokenJwtDTO;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.user.Usuario;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;

  public LoginService(AuthenticationManager authenticationManager, TokenService tokenService) {
    this.authenticationManager = authenticationManager;
    this.tokenService = tokenService;
  }

  public TokenJwtDTO login(UsuarioAutenticacaoDTO request) {
    var credentials = new UsernamePasswordAuthenticationToken(request.email(), request.senha());
    var authentication = authenticationManager.authenticate(credentials);
    var token = tokenService.geraToken((Usuario) authentication.getPrincipal());
    return new TokenJwtDTO(token);
  }
}
