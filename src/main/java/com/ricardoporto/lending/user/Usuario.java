package com.ricardoporto.lending.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity(name = "User")
@Table(name = "usuarios")
@NoArgsConstructor
@Getter
@Setter
public class Usuario implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nome;
  private String sobrenome;

  @Column(unique = true)
  private String email;

  private String senha;
  private boolean isConfirmado;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "security_version", nullable = false)
  private int securityVersion;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "usuarios_perfis",
      joinColumns = @JoinColumn(name = "usuarios_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "perfis_id", referencedColumnName = "id"))
  private List<Perfil> perfis;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return perfis;
  }

  @Override
  public String getPassword() {
    return senha;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return lockedUntil == null || !lockedUntil.isAfter(Instant.now());
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return isConfirmado;
  }
}
