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
  private final PerfilRepository perfilRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      PerfilRepository perfilRepository,
      PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.perfilRepository = perfilRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse register(RegistrationRequest request) {
    var normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    if (usuarioRepository.existsByEmail(normalizedEmail)) {
      throw new ApiException(
          HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "The email is already registered.");
    }

    var usuario = new Usuario();
    usuario.setEmail(normalizedEmail);
    usuario.setSenha(passwordEncoder.encode(request.password()));
    usuario.setConfirmado(true);
    var studentProfile =
        perfilRepository
            .findByNome(Role.STUDENT.authority())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The STUDENT role was not initialized by the database migration."));
    usuario.setPerfis(List.of(studentProfile));
    var saved = usuarioRepository.save(usuario);
    return new UserResponse(saved.getId(), saved.getEmail(), saved.isConfirmado());
  }
}
