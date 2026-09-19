package com.ricardoporto.lending.user;

import com.ricardoporto.lending.shared.exception.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UsuarioDto register(UsuarioCadastroDto request) {
    var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    if (usuarioRepository.existsByEmail(normalizedEmail)) {
      throw new ApiException(
          HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "The email is already registered.");
    }

    var usuario = new Usuario();
    usuario.setEmail(normalizedEmail);
    usuario.setSenha(passwordEncoder.encode(request.senha()));
    usuario.setConfirmado(false);
    usuario.setPerfis(List.of());
    var saved = usuarioRepository.save(usuario);
    return new UsuarioDto(saved.getId(), saved.getEmail(), saved.isConfirmado());
  }
}
