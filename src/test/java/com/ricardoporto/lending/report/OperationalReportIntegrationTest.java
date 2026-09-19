package com.ricardoporto.lending.report;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ricardoporto.lending.loan.Loan;
import com.ricardoporto.lending.loan.LoanRepository;
import com.ricardoporto.lending.loan.LoanStatus;
import com.ricardoporto.lending.reservation.Reservation;
import com.ricardoporto.lending.reservation.ReservationRepository;
import com.ricardoporto.lending.reservation.ReservationStatus;
import com.ricardoporto.lending.resource.Resource;
import com.ricardoporto.lending.resource.ResourceItem;
import com.ricardoporto.lending.resource.ResourceItemRepository;
import com.ricardoporto.lending.resource.ResourceItemStatus;
import com.ricardoporto.lending.resource.ResourceRepository;
import com.ricardoporto.lending.resource.ResourceType;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OperationalReportIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private TokenService tokenService;
  @Autowired private UsuarioRepository userRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private ResourceItemRepository itemRepository;
  @Autowired private LoanRepository loanRepository;
  @Autowired private ReservationRepository reservationRepository;

  @BeforeEach
  void createOperationalData() {
    var now = Instant.now();
    var resource = new Resource();
    resource.setId(UUID.randomUUID());
    resource.setName("Report laptop " + UUID.randomUUID());
    resource.setIdentifier("REPORT-" + UUID.randomUUID());
    resource.setType(ResourceType.LAPTOP);
    resource.setLoanable(true);
    resource.setCreatedAt(now);
    resource.setUpdatedAt(now);
    resourceRepository.save(resource);

    var item = new ResourceItem();
    item.setId(UUID.randomUUID());
    item.setResource(resource);
    item.setAssetTag("REPORT-ITEM-" + UUID.randomUUID());
    item.setStatus(ResourceItemStatus.ON_LOAN);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    itemRepository.save(item);

    var loan = new Loan();
    loan.setId(UUID.randomUUID());
    loan.setBorrower(userRepository.findByEmail("student1@email.com"));
    loan.setResourceItem(item);
    loan.setStatus(LoanStatus.ACTIVE);
    loan.setRequestedAt(now.minus(3, ChronoUnit.DAYS));
    loan.setBorrowedAt(now.minus(2, ChronoUnit.DAYS));
    loan.setDueAt(now.plus(5, ChronoUnit.DAYS));
    loanRepository.save(loan);

    var reservation = new Reservation();
    reservation.setId(UUID.randomUUID());
    reservation.setUser(userRepository.findByEmail("student2@email.com"));
    reservation.setResource(resource);
    reservation.setStatus(ReservationStatus.FULFILLED);
    reservation.setCreatedAt(now.minus(3, ChronoUnit.HOURS));
    reservation.setReadyAt(now.minus(1, ChronoUnit.HOURS));
    reservationRepository.save(reservation);
    resourceRepository.flush();
    itemRepository.flush();
    loanRepository.flush();
    reservationRepository.flush();
  }

  @Test
  void exposesAllOperationalViewsToStaff() throws Exception {
    mvc.perform(
            get("/api/v1/reports/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeLoans", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.unavailableItems", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.averageReservationWaitSeconds", greaterThan(0.0)));
    mvc.perform(
            get("/api/v1/reports/loans?status=ACTIVE")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    mvc.perform(
            get("/api/v1/reports/resource-utilization")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].currentUtilizationPercent").exists());
    mvc.perform(
            get("/api/v1/reports/reservation-wait")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sampleSize", greaterThanOrEqualTo(1)));
    mvc.perform(
            get("/api/v1/reports/popular-resources?limit=5")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalRequests", greaterThanOrEqualTo(2)));
    mvc.perform(
            get("/api/v1/reports/unavailable-items")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").exists());
  }

  @Test
  void exportsCsvAndRejectsStudentAccess() throws Exception {
    mvc.perform(
            get("/api/v1/reports/export.csv?report=LOANS&loanStatus=ACTIVE")
                .header(HttpHeaders.AUTHORIZATION, bearer("admin@email.com")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.containsString("loans.csv")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("loan_id,status,borrower_email")));

    mvc.perform(
            get("/api/v1/reports/dashboard")
                .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com")))
        .andExpect(status().isForbidden());
  }

  private String bearer(String email) {
    return "Bearer " + tokenService.generateAccessToken(userRepository.findByEmail(email));
  }
}
