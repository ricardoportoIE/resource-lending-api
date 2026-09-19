package com.ricardoporto.lending.shared.security;

import com.ricardoporto.lending.user.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
  public Usuario currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
      throw new IllegalStateException("Authenticated user is not available.");
    }
    return usuario;
  }

  public boolean isStaffOrAdmin() {
    return currentUser().getAuthorities().stream()
        .anyMatch(
            authority ->
                authority.getAuthority().equals("ROLE_STAFF")
                    || authority.getAuthority().equals("ROLE_ADMIN"));
  }
}
