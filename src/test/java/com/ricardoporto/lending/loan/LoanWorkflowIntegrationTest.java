package com.ricardoporto.lending.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.audit.AuditEventRepository;
import com.ricardoporto.lending.idempotency.IdempotencyRecordRepository;
import com.ricardoporto.lending.idempotency.IdempotencyStatus;
import com.ricardoporto.lending.outbox.OutboxEventRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoanWorkflowIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TokenService tokenService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private ResourceItemRepository itemRepository;
  @Autowired private LoanRepository loanRepository;
  @Autowired private AuditEventRepository auditEventRepository;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private IdempotencyRecordRepository idempotencyRecordRepository;

  private Resource resource;
  private ResourceItem item;

  @BeforeEach
  void createInventory() {
    var now = Instant.now();
    resource = new Resource();
    resource.setId(UUID.randomUUID());
    resource.setName("Workflow test resource");
    resource.setType(ResourceType.BOOK);
    resource.setIdentifier("WORKFLOW-" + UUID.randomUUID());
    resource.setLoanable(true);
    resource.setCreatedAt(now);
    resource.setUpdatedAt(now);
    resourceRepository.save(resource);
    item = createItem("WORKFLOW-ITEM-" + UUID.randomUUID(), ResourceItemStatus.AVAILABLE);
  }

  @Test
  void executesExplicitWorkflowWithPolicyOwnershipAndAudit() throws Exception {
    var requested = requestLoan(item.getId(), "student1@email.com", 201);
    var loanId = UUID.fromString(requested.get("id").textValue());

    transition(loanId, "collect", "staff@email.com", 409)
        .andExpect(jsonPath("$.code").value("INVALID_LOAN_TRANSITION"));
    mvc.perform(
            get("/api/v1/loans/" + loanId)
                .header(HttpHeaders.AUTHORIZATION, bearer("student2@email.com")))
        .andExpect(status().isForbidden());

    transition(loanId, "approve", "staff@email.com", 200)
        .andExpect(jsonPath("$.status").value("APPROVED"));
    var approvalEvent =
        outboxEventRepository.findByDeduplicationKey("LOAN_APPROVED:" + loanId).orElseThrow();
    assertEquals("LOAN_APPROVED", approvalEvent.getEventType());
    mvc.perform(
            get("/api/v1/admin/outbox-events?status=PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/admin/outbox-events?status=PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer("admin@email.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].eventType").value("LOAN_APPROVED"));
    var active =
        transition(loanId, "collect", "staff@email.com", 200)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn()
            .getResponse();
    var dueAt = objectMapper.readTree(active.getContentAsString()).get("dueAt").asText();
    assertNotNull(dueAt);
    assertTrue(Instant.parse(dueAt).isAfter(Instant.now().plus(20, ChronoUnit.DAYS)));
    assertEquals(
        ResourceItemStatus.ON_LOAN,
        itemRepository.findById(item.getId()).orElseThrow().getStatus());

    requestLoan(item.getId(), "student1@email.com", 409);
    transition(loanId, "return", "staff@email.com", 200)
        .andExpect(jsonPath("$.status").value("RETURNED"));
    assertEquals(
        ResourceItemStatus.AVAILABLE,
        itemRepository.findById(item.getId()).orElseThrow().getStatus());
    assertTrue(auditEventRepository.count() >= 4);
  }

  @Test
  void rejectsNewLoanWhenBorrowerHasOverdueLoan() throws Exception {
    var borrower = usuarioRepository.findByEmail("student1@email.com");
    var overdueItem = createItem("OVERDUE-ITEM-" + UUID.randomUUID(), ResourceItemStatus.ON_LOAN);
    var overdue = new Loan();
    overdue.setId(UUID.randomUUID());
    overdue.setBorrower(borrower);
    overdue.setResourceItem(overdueItem);
    overdue.setStatus(LoanStatus.ACTIVE);
    overdue.setRequestedAt(Instant.now().minus(30, ChronoUnit.DAYS));
    overdue.setBorrowedAt(Instant.now().minus(30, ChronoUnit.DAYS));
    overdue.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
    loanRepository.save(overdue);

    var response = requestLoan(item.getId(), "student1@email.com", 409);
    assertEquals("BORROWER_HAS_OVERDUE_LOAN", response.get("code").textValue());
    assertEquals(
        LoanStatus.OVERDUE, loanRepository.findById(overdue.getId()).orElseThrow().getStatus());
  }

  @Test
  void rejectsNewLoanWhenStudentReachedPolicyLimit() throws Exception {
    var borrower = usuarioRepository.findByEmail("student1@email.com");
    for (int index = 0; index < 3; index++) {
      var committed = new Loan();
      committed.setId(UUID.randomUUID());
      committed.setBorrower(borrower);
      committed.setResourceItem(
          createItem(
              "LIMIT-ITEM-" + index + "-" + UUID.randomUUID(), ResourceItemStatus.AVAILABLE));
      committed.setStatus(LoanStatus.APPROVED);
      committed.setRequestedAt(Instant.now());
      committed.setApprovedAt(Instant.now());
      loanRepository.save(committed);
    }

    var response = requestLoan(item.getId(), "student1@email.com", 409);
    assertEquals("LOAN_LIMIT_REACHED", response.get("code").textValue());
  }

  @Test
  void replaysCommandResponseAndRejectsKeyReuseWithDifferentPayload() throws Exception {
    var key = "loan-request-" + UUID.randomUUID();
    var body = "{\"resourceItemId\":\"" + item.getId() + "\"}";
    var before = loanRepository.count();

    var original =
        mvc.perform(
                post("/api/v1/loans")
                    .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com"))
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var replay =
        mvc.perform(
                post("/api/v1/loans")
                    .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com"))
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(
                result ->
                    assertEquals("true", result.getResponse().getHeader("Idempotency-Replayed")))
            .andReturn()
            .getResponse();

    assertEquals(original.getContentAsString(), replay.getContentAsString());
    assertEquals(before + 1, loanRepository.count());
    var record =
        idempotencyRecordRepository
            .findByIdempotencyKeyAndUserIdAndEndpoint(key, 3L, "/api/v1/loans")
            .orElseThrow();
    assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());
    assertEquals(201, record.getHttpStatus());

    mvc.perform(
            post("/api/v1/loans")
                .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com"))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceItemId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
  }

  private com.fasterxml.jackson.databind.JsonNode requestLoan(
      UUID itemId, String email, int expectedStatus) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/loans")
                    .header(HttpHeaders.AUTHORIZATION, bearer(email))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"resourceItemId\":\"" + itemId + "\"}"))
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString());
  }

  private org.springframework.test.web.servlet.ResultActions transition(
      UUID loanId, String action, String email, int expectedStatus) throws Exception {
    return mvc.perform(
            post("/api/v1/loans/" + loanId + "/" + action)
                .header(HttpHeaders.AUTHORIZATION, bearer(email)))
        .andExpect(status().is(expectedStatus));
  }

  private String bearer(String email) {
    return "Bearer " + tokenService.generateAccessToken(usuarioRepository.findByEmail(email));
  }

  private ResourceItem createItem(String assetTag, ResourceItemStatus status) {
    var now = Instant.now();
    var created = new ResourceItem();
    created.setId(UUID.randomUUID());
    created.setResource(resource);
    created.setAssetTag(assetTag);
    created.setStatus(status);
    created.setCreatedAt(now);
    created.setUpdatedAt(now);
    return itemRepository.save(created);
  }
}
