package com.ricardoporto.lending.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "revoked_access_tokens")
@Getter
@Setter
public class RevokedAccessToken {
  @Id
  @Column(name = "jwt_id", length = 64)
  private String jwtId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at", nullable = false)
  private Instant revokedAt;
}
