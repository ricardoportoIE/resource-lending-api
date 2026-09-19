package com.ricardoporto.lending.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource_items")
@Getter
@Setter
@NoArgsConstructor
public class ResourceItem {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "resource_id", nullable = false)
  private Resource resource;

  @Column(name = "asset_tag", nullable = false, unique = true, length = 100)
  private String assetTag;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ResourceItemStatus status;

  @Version private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
