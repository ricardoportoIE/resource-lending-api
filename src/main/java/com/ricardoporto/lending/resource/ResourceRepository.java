package com.ricardoporto.lending.resource;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
  boolean existsByIdentifier(String identifier);

  @Query(
      """
      select distinct resource from Resource resource
      left join ResourceItem item on item.resource = resource
      where (:type is null or resource.type = :type)
        and (coalesce(:category, '') = '' or lower(resource.category) = lower(:category))
        and (:status is null or item.status = :status)
      """)
  Page<Resource> findFiltered(
      @Param("type") ResourceType type,
      @Param("category") String category,
      @Param("status") ResourceItemStatus status,
      Pageable pageable);
}
