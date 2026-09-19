package com.ricardoporto.lending.loan;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
  List<Loan> findAllByBorrowerIdOrderByRequestedAtDesc(Long borrowerId);

  long countByBorrowerIdAndStatusIn(Long borrowerId, Collection<LoanStatus> statuses);

  boolean existsByBorrowerIdAndStatusInAndDueAtBefore(
      Long borrowerId, Collection<LoanStatus> statuses, Instant now);

  boolean existsByResourceItemIdAndStatusIn(UUID resourceItemId, Collection<LoanStatus> statuses);

  List<Loan> findAllByStatusAndDueAtBefore(LoanStatus status, Instant now);
}
