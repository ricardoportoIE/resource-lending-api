package com.ricardoporto.lending.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select token from RefreshToken token join fetch token.usuario where token.tokenHash = :hash")
  Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);

  @Modifying
  @Query(
      "update RefreshToken token set token.revokedAt = :now "
          + "where token.usuario.id = :userId and token.revokedAt is null")
  int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
