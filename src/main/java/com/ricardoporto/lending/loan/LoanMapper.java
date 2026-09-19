package com.ricardoporto.lending.loan;

public final class LoanMapper {
  private LoanMapper() {}

  public static LoanResponse toResponse(Loan loan) {
    return new LoanResponse(
        loan.getId(),
        loan.getBorrower().getId(),
        loan.getBorrower().getEmail(),
        loan.getResourceItem().getId(),
        loan.getResourceItem().getAssetTag(),
        loan.getStatus(),
        loan.getRequestedAt(),
        loan.getApprovedAt(),
        loan.getBorrowedAt(),
        loan.getDueAt(),
        loan.getReturnedAt(),
        loan.getApprover() == null ? null : loan.getApprover().getId(),
        loan.getVersion());
  }
}
