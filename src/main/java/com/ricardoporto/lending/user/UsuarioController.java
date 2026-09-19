package com.ricardoporto.lending.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user profile")
public class UsuarioController {
  @GetMapping("/me")
  public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal Usuario usuario) {
    var roles =
        usuario.getAuthorities().stream()
            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
            .collect(Collectors.toUnmodifiableSet());
    return ResponseEntity.ok(
        new CurrentUserResponse(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNome(),
            usuario.getSobrenome(),
            Set.copyOf(roles)));
  }
}
