package com.ricardoporto.lending.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
public class Resource {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ResourceType type;

  @Column(length = 100)
  private String category;

  @Column(unique = true, length = 100)
  private String identifier;

  @Column(nullable = false)
  private boolean loanable;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
