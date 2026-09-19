package com.ricardoporto.lending.resource;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, UUID> {
  List<ResourceItem> findAllByResourceIdOrderByAssetTag(UUID resourceId);

  boolean existsByAssetTag(String assetTag);
}
