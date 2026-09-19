package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.shared.security.TokenJwtDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AutenticacaoController {

  private final LoginService loginService;

  public AutenticacaoController(LoginService loginService) {
    this.loginService = loginService;
  }

  @PostMapping("/login")
  public ResponseEntity<TokenJwtDTO> efetuarLogin(
      @Valid @RequestBody UsuarioAutenticacaoDTO request) {
    return ResponseEntity.ok(loginService.login(request));
  }
}
