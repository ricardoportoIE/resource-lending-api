package com.ricardoporto.lending.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @PostMapping("/cadastrar")
  public ResponseEntity<UsuarioDto> cadastrarUsuario(
      @Valid @RequestBody UsuarioCadastroDto request) {
    return ResponseEntity.status(201).body(usuarioService.register(request));
  }
}
