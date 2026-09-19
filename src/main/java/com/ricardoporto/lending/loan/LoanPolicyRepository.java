package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.resource.ResourceType;
import com.ricardoporto.lending.user.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicy, UUID> {
  Optional<LoanPolicy> findByRoleAndResourceType(Role role, ResourceType resourceType);
}
