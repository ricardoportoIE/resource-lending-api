package com.ricardoporto.lending.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ricardoporto.lending.loan.CreateLoanRequest;
import com.ricardoporto.lending.loan.Loan;
import com.ricardoporto.lending.loan.LoanRepository;
import com.ricardoporto.lending.loan.LoanStatus;
import com.ricardoporto.lending.loan.LoanWorkflowService;
import com.ricardoporto.lending.resource.Resource;
import com.ricardoporto.lending.resource.ResourceItem;
import com.ricardoporto.lending.resource.ResourceItemRepository;
import com.ricardoporto.lending.resource.ResourceItemStatus;
import com.ricardoporto.lending.resource.ResourceRepository;
import com.ricardoporto.lending.resource.ResourceType;
import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.Usuario;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ReservationAndConcurrencyIntegrationTest extends PostgresIntegrationTest {
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private ResourceItemRepository itemRepository;
  @Autowired private LoanRepository loanRepository;
  @Autowired private LoanWorkflowService loanWorkflowService;
  @Autowired private ReservationService reservationService;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private UsuarioRepository usuarioRepository;

  private Resource resource;
  private ResourceItem item;

  @BeforeEach
  void createInventory() {
    var now = Instant.now();
    resource = new Resource();
    resource.setId(UUID.randomUUID());
    resource.setName("Concurrency resource");
    resource.setType(ResourceType.EQUIPMENT);
    resource.setIdentifier("CONCURRENT-" + UUID.randomUUID());
    resource.setLoanable(true);
    resource.setCreatedAt(now);
    resource.setUpdatedAt(now);
    resourceRepository.save(resource);

    item = new ResourceItem();
    item.setId(UUID.randomUUID());
    item.setResource(resource);
    item.setAssetTag("CONCURRENT-ITEM-" + UUID.randomUUID());
    item.setStatus(ResourceItemStatus.AVAILABLE);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    itemRepository.save(item);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void onlyOneConcurrentRequestCanClaimTheSameItem() throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(() -> requestConcurrently("student1@email.com", ready, start));
      var second = executor.submit(() -> requestConcurrently("student2@email.com", ready, start));
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      var outcomes =
          java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
      assertEquals(1, outcomes.stream().filter("SUCCESS"::equals).count());
      assertEquals(1, outcomes.stream().filter("RESOURCE_UNAVAILABLE"::equals).count());
      assertEquals(
          1,
          loanRepository.countByBorrowerIdAndStatusIn(
                  usuarioRepository.findByEmail("student1@email.com").getId(),
                  java.util.EnumSet.of(LoanStatus.REQUESTED))
              + loanRepository.countByBorrowerIdAndStatusIn(
                  usuarioRepository.findByEmail("student2@email.com").getId(),
                  java.util.EnumSet.of(LoanStatus.REQUESTED)));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void promotesQueueAfterReturnCancellationAndExpiry() {
    var borrower = usuarioRepository.findByEmail("student1@email.com");
    item.setStatus(ResourceItemStatus.ON_LOAN);
    itemRepository.save(item);
    var loan = new Loan();
    loan.setId(UUID.randomUUID());
    loan.setBorrower(borrower);
    loan.setResourceItem(item);
    loan.setStatus(LoanStatus.ACTIVE);
    loan.setRequestedAt(Instant.now().minus(3, ChronoUnit.DAYS));
    loan.setBorrowedAt(Instant.now().minus(2, ChronoUnit.DAYS));
    loan.setDueAt(Instant.now().plus(10, ChronoUnit.DAYS));
    loanRepository.save(loan);

    authenticate("student1@email.com");
    var first = reservationService.reserve(resource.getId());
    authenticate("student2@email.com");
    var second = reservationService.reserve(resource.getId());
    assertEquals(ReservationStatus.WAITING, first.status());
    assertEquals(ReservationStatus.WAITING, second.status());

    authenticate("staff@email.com");
    loanWorkflowService.returnLoan(loan.getId());
    var promotedFirst = reservationRepository.findById(first.id()).orElseThrow();
    assertEquals(ReservationStatus.READY, promotedFirst.getStatus());
    assertEquals(
        ResourceItemStatus.RESERVED,
        itemRepository.findById(item.getId()).orElseThrow().getStatus());

    authenticate("student1@email.com");
    reservationService.cancel(first.id());
    var promotedSecond = reservationRepository.findById(second.id()).orElseThrow();
    assertEquals(ReservationStatus.READY, promotedSecond.getStatus());

    promotedSecond.setExpiresAt(Instant.now().minusSeconds(1));
    reservationRepository.save(promotedSecond);
    reservationService.expireReadyReservations();
    assertEquals(
        ReservationStatus.EXPIRED,
        reservationRepository.findById(second.id()).orElseThrow().getStatus());
    assertEquals(
        ResourceItemStatus.AVAILABLE,
        itemRepository.findById(item.getId()).orElseThrow().getStatus());
  }

  private String requestConcurrently(String email, CountDownLatch ready, CountDownLatch start) {
    authenticate(email);
    ready.countDown();
    try {
      start.await(5, TimeUnit.SECONDS);
      loanWorkflowService.request(new CreateLoanRequest(item.getId(), null));
      return "SUCCESS";
    } catch (ApiException exception) {
      return exception.getCode();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return "INTERRUPTED";
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void authenticate(String email) {
    Usuario user = usuarioRepository.findByEmail(email);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }
}
