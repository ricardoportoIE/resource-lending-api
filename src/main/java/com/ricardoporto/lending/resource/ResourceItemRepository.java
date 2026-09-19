package com.ricardoporto.lending.resource;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, UUID> {
  List<ResourceItem> findAllByResourceIdOrderByAssetTag(UUID resourceId);

  boolean existsByAssetTag(String assetTag);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select item from ResourceItem item join fetch item.resource where item.id = :id")
  Optional<ResourceItem> findByIdForUpdate(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ResourceItem> findFirstByResourceIdAndStatusOrderByAssetTag(
      UUID resourceId, ResourceItemStatus status);
}
