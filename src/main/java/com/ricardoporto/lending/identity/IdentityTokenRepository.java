package com.ricardoporto.lending.identity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityTokenRepository extends JpaRepository<IdentityToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select token from IdentityToken token join fetch token.usuario "
          + "where token.tokenHash = :hash and token.purpose = :purpose")
  Optional<IdentityToken> findForUpdate(
      @Param("hash") String hash, @Param("purpose") IdentityTokenPurpose purpose);
}
