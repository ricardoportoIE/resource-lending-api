package com.ricardoporto.lending.shared.security;

import com.ricardoporto.lending.user.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityFilter extends OncePerRequestFilter {
  private final TokenService tokenService;
  private final UsuarioRepository repository;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public SecurityFilter(
      TokenService tokenService,
      UsuarioRepository repository,
      AuthenticationEntryPoint authenticationEntryPoint) {
    this.tokenService = tokenService;
    this.repository = repository;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var token = bearerToken(request);
    if (token != null) {
      try {
        var usuario = repository.findByEmail(tokenService.getSubject(token));
        if (usuario == null || !usuario.isEnabled()) {
          authenticationEntryPoint.commence(
              request, response, new BadCredentialsException("Invalid access token."));
          return;
        }
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
      } catch (RuntimeException exception) {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
            request, response, new BadCredentialsException("Invalid access token.", exception));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private String bearerToken(HttpServletRequest request) {
    var header = request.getHeader("Authorization");
    return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
  }
}
