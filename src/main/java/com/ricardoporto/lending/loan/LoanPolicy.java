package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.resource.ResourceType;
import com.ricardoporto.lending.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan_policies")
@Getter
@Setter
@NoArgsConstructor
public class LoanPolicy {
  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false, length = 30)
  private ResourceType resourceType;

  @Column(name = "max_active_loans", nullable = false)
  private int maxActiveLoans;

  @Column(name = "duration_days", nullable = false)
  private int durationDays;
}
