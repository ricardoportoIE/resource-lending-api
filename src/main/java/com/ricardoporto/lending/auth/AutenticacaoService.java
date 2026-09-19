package com.ricardoporto.lending.auth;

import com.ricardoporto.lending.user.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // indica que essa classe deve ser adicionada ao Contexto do aplicativo como um Bean da
// camada de serviço de dados
public class AutenticacaoService implements UserDetailsService {
  private final UsuarioRepository repository;

  // indica ao Spring Boot que ele deve injetar essa dependência para a classe funcionar
  public AutenticacaoService(UsuarioRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var usuario = repository.findByEmail(username);
    if (usuario == null) {
      throw new UsernameNotFoundException("User not found.");
    }
    return usuario;
  }
}
