package com.ricardoporto.lending.user;

import com.ricardoporto.lending.identity.IdentityService;
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
  private final IdentityService identityService;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      PerfilRepository perfilRepository,
      PasswordEncoder passwordEncoder,
      IdentityService identityService) {
    this.usuarioRepository = usuarioRepository;
    this.perfilRepository = perfilRepository;
    this.passwordEncoder = passwordEncoder;
    this.identityService = identityService;
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
    usuario.setConfirmado(false);
    var studentProfile =
        perfilRepository
            .findByNome(Role.STUDENT.authority())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The STUDENT role was not initialized by the database migration."));
    usuario.setPerfis(List.of(studentProfile));
    var saved = usuarioRepository.save(usuario);
    identityService.sendEmailConfirmation(saved);
    return new UserResponse(saved.getId(), saved.getEmail(), saved.isConfirmado());
  }
}
