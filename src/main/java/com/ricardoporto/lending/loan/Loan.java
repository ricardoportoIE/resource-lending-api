package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.resource.ResourceItem;
import com.ricardoporto.lending.user.Usuario;
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
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
public class Loan {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "borrower_id", nullable = false)
  private Usuario borrower;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "resource_item_id", nullable = false)
  private ResourceItem resourceItem;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LoanStatus status;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "borrowed_at")
  private Instant borrowedAt;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approver_id")
  private Usuario approver;

  @Version private long version;
}
