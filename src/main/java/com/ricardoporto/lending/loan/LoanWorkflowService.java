package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.audit.AuditService;
import com.ricardoporto.lending.outbox.OutboxService;
import com.ricardoporto.lending.reservation.ReservationService;
import com.ricardoporto.lending.resource.ResourceItem;
import com.ricardoporto.lending.resource.ResourceItemRepository;
import com.ricardoporto.lending.resource.ResourceItemStatus;
import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import com.ricardoporto.lending.shared.security.AuthorizationService;
import com.ricardoporto.lending.user.Role;
import com.ricardoporto.lending.user.Usuario;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanWorkflowService {
  private static final EnumSet<LoanStatus> COMMITTED_STATUSES =
      EnumSet.of(LoanStatus.APPROVED, LoanStatus.ACTIVE, LoanStatus.OVERDUE);
  private static final EnumSet<LoanStatus> OPEN_ITEM_STATUSES =
      EnumSet.of(LoanStatus.REQUESTED, LoanStatus.APPROVED, LoanStatus.ACTIVE, LoanStatus.OVERDUE);

  private final LoanRepository loanRepository;
  private final LoanPolicyRepository policyRepository;
  private final ResourceItemRepository itemRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuthorizationService authorizationService;
  private final AuditService auditService;
  private final ReservationService reservationService;
  private final OutboxService outboxService;

  public LoanWorkflowService(
      LoanRepository loanRepository,
      LoanPolicyRepository policyRepository,
      ResourceItemRepository itemRepository,
      UsuarioRepository usuarioRepository,
      AuthorizationService authorizationService,
      AuditService auditService,
      ReservationService reservationService,
      OutboxService outboxService) {
    this.loanRepository = loanRepository;
    this.policyRepository = policyRepository;
    this.itemRepository = itemRepository;
    this.usuarioRepository = usuarioRepository;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.reservationService = reservationService;
    this.outboxService = outboxService;
  }

  @Transactional(noRollbackFor = ApiException.class)
  public LoanResponse request(CreateLoanRequest request) {
    markOverdueLoans();
    var borrower = resolveBorrower(request.borrowerId());
    var item = requireItemForUpdate(request.resourceItemId());
    reservationService.claimReadyReservation(borrower, item);
    assertCanBorrow(borrower, item);

    var loan = new Loan();
    loan.setId(UUID.randomUUID());
    loan.setBorrower(borrower);
    loan.setResourceItem(item);
    loan.setStatus(LoanStatus.REQUESTED);
    loan.setRequestedAt(Instant.now());
    var saved = loanRepository.save(loan);
    auditService.record("LOAN_REQUESTED", "Loan", saved.getId(), "status=REQUESTED");
    return LoanMapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<LoanResponse> findAll() {
    var current = authorizationService.currentUser();
    var loans =
        authorizationService.isStaffOrAdmin()
            ? loanRepository.findAll()
            : loanRepository.findAllByBorrowerIdOrderByRequestedAtDesc(current.getId());
    return loans.stream().map(LoanMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public LoanResponse findById(UUID id) {
    var loan = requireLoan(id);
    assertCanRead(loan);
    return LoanMapper.toResponse(loan);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public LoanResponse approve(UUID id) {
    var loan = requireLoan(id);
    transition(loan, LoanStatus.REQUESTED, LoanStatus.APPROVED);
    if (loan.getResourceItem().getStatus() != ResourceItemStatus.AVAILABLE) {
      throw conflict("RESOURCE_UNAVAILABLE", "The selected resource item is not available.");
    }
    loan.setApprover(authorizationService.currentUser());
    loan.setApprovedAt(Instant.now());
    return saveAndAudit(loan, "LOAN_APPROVED");
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public LoanResponse reject(UUID id) {
    var loan = requireLoan(id);
    transition(loan, LoanStatus.REQUESTED, LoanStatus.REJECTED);
    loan.setApprover(authorizationService.currentUser());
    return saveAndAudit(loan, "LOAN_REJECTED");
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public LoanResponse collect(UUID id) {
    var loan = requireLoan(id);
    transition(loan, LoanStatus.APPROVED, LoanStatus.ACTIVE);
    var item = requireItemForUpdate(loan.getResourceItem().getId());
    if (item.getStatus() != ResourceItemStatus.AVAILABLE) {
      throw conflict("RESOURCE_UNAVAILABLE", "The selected resource item is not available.");
    }
    var policy = policyFor(loan.getBorrower(), item);
    var now = Instant.now();
    loan.setBorrowedAt(now);
    loan.setDueAt(now.plus(policy.getDurationDays(), ChronoUnit.DAYS));
    item.setStatus(ResourceItemStatus.ON_LOAN);
    item.setUpdatedAt(now);
    itemRepository.save(item);
    return saveAndAudit(loan, "LOAN_COLLECTED");
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public LoanResponse returnLoan(UUID id) {
    var loan = requireLoan(id);
    if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
      throw invalidTransition(loan, LoanStatus.RETURNED);
    }
    loan.setStatus(LoanStatus.RETURNED);
    loan.setReturnedAt(Instant.now());
    var item = requireItemForUpdate(loan.getResourceItem().getId());
    item.setStatus(ResourceItemStatus.AVAILABLE);
    item.setUpdatedAt(Instant.now());
    itemRepository.save(item);
    reservationService.promoteNext(item);
    return saveAndAudit(loan, "LOAN_RETURNED");
  }

  @Transactional
  public LoanResponse cancel(UUID id) {
    var loan = requireLoan(id);
    var current = authorizationService.currentUser();
    if (!authorizationService.isStaffOrAdmin()
        && !loan.getBorrower().getId().equals(current.getId())) {
      throw new AccessDeniedException("Only the borrower or staff can cancel this loan.");
    }
    if (loan.getStatus() != LoanStatus.REQUESTED && loan.getStatus() != LoanStatus.APPROVED) {
      throw invalidTransition(loan, LoanStatus.CANCELLED);
    }
    loan.setStatus(LoanStatus.CANCELLED);
    return saveAndAudit(loan, "LOAN_CANCELLED");
  }

  private void assertCanBorrow(Usuario borrower, ResourceItem item) {
    if (!borrower.isEnabled()) {
      throw conflict("BORROWER_NOT_ACTIVE", "The borrower account is not active.");
    }
    if (!item.getResource().isLoanable()
        || item.getStatus() != ResourceItemStatus.AVAILABLE
        || loanRepository.existsByResourceItemIdAndStatusIn(item.getId(), OPEN_ITEM_STATUSES)) {
      throw conflict("RESOURCE_UNAVAILABLE", "The selected resource item is not available.");
    }
    if (loanRepository.existsByBorrowerIdAndStatusInAndDueAtBefore(
        borrower.getId(), EnumSet.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE), Instant.now())) {
      throw conflict("BORROWER_HAS_OVERDUE_LOAN", "The borrower has an overdue loan.");
    }
    var policy = policyFor(borrower, item);
    if (loanRepository.countByBorrowerIdAndStatusIn(borrower.getId(), COMMITTED_STATUSES)
        >= policy.getMaxActiveLoans()) {
      throw conflict("LOAN_LIMIT_REACHED", "The borrower reached the active loan limit.");
    }
  }

  private void markOverdueLoans() {
    loanRepository
        .findAllByStatusAndDueAtBefore(LoanStatus.ACTIVE, Instant.now())
        .forEach(
            loan -> {
              loan.setStatus(LoanStatus.OVERDUE);
              auditService.record("LOAN_OVERDUE", "Loan", loan.getId(), "status=OVERDUE");
            });
  }

  private Usuario resolveBorrower(Long requestedBorrowerId) {
    var current = authorizationService.currentUser();
    if (requestedBorrowerId == null || requestedBorrowerId.equals(current.getId())) return current;
    if (!authorizationService.isStaffOrAdmin()) {
      throw new AccessDeniedException("Students can only request loans for themselves.");
    }
    return usuarioRepository
        .findById(requestedBorrowerId)
        .orElseThrow(() -> new ResourceNotFoundException("User", requestedBorrowerId));
  }

  private LoanPolicy policyFor(Usuario borrower, ResourceItem item) {
    var role = primaryRole(borrower);
    return policyRepository
        .findByRoleAndResourceType(role, item.getResource().getType())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Loan policy is missing for " + role + " and " + item.getResource().getType()));
  }

  private Role primaryRole(Usuario user) {
    var authorities =
        user.getAuthorities().stream().map(authority -> authority.getAuthority()).toList();
    if (authorities.contains(Role.ADMIN.authority())) return Role.ADMIN;
    if (authorities.contains(Role.STAFF.authority())) return Role.STAFF;
    return Role.STUDENT;
  }

  private LoanResponse saveAndAudit(Loan loan, String action) {
    var saved = loanRepository.save(loan);
    auditService.record(action, "Loan", saved.getId(), "status=" + saved.getStatus());
    if ("LOAN_APPROVED".equals(action)) {
      outboxService.publish(
          action,
          "Loan",
          saved.getId(),
          Map.of(
              "loanId", saved.getId(),
              "borrowerEmail", saved.getBorrower().getEmail(),
              "resourceItemId", saved.getResourceItem().getId(),
              "assetTag", saved.getResourceItem().getAssetTag()),
          action + ":" + saved.getId());
    }
    return LoanMapper.toResponse(saved);
  }

  private void transition(Loan loan, LoanStatus expected, LoanStatus target) {
    if (loan.getStatus() != expected) throw invalidTransition(loan, target);
    loan.setStatus(target);
  }

  private ApiException invalidTransition(Loan loan, LoanStatus target) {
    return conflict(
        "INVALID_LOAN_TRANSITION",
        "Loan cannot transition from " + loan.getStatus() + " to " + target + ".");
  }

  private ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }

  private void assertCanRead(Loan loan) {
    if (!authorizationService.isStaffOrAdmin()
        && !loan.getBorrower().getId().equals(authorizationService.currentUser().getId())) {
      throw new AccessDeniedException("Loan does not belong to the authenticated user.");
    }
  }

  private Loan requireLoan(UUID id) {
    return loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Loan", id));
  }

  private ResourceItem requireItemForUpdate(UUID id) {
    return itemRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource item", id));
  }
}
