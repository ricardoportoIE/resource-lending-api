package com.ricardoporto.lending.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UsuarioRepository users;
  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void resetLoginProtection() {
    jdbc.update(
        "update usuarios set failed_login_attempts = 0, locked_until = null where email = ?",
        "student2@email.com");
  }

  @Test
  void rejectsCrossOriginCommandsWhenNoOriginIsAllowlisted() throws Exception {
    mvc.perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://attacker.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

    mvc.perform(
            post("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://attacker.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("student2@email.com", "123")))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void rejectsMalformedBearerTokensWithoutLeakingParserDetails() throws Exception {
    for (var token : new String[] {"not-a-jwt", "eyJhbGciOiJub25lIn0.e30.", "a.b.c"}) {
      mvc.perform(get("/api/v1/resources").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
          .andExpect(jsonPath("$.detail").value("A valid access token is required."));
    }
  }

  @Test
  void enforcesPrivilegesAcrossReportsAdministrationAndActuator() throws Exception {
    var student = bearer("student2@email.com");
    var staff = bearer("staff@email.com");
    var admin = bearer("admin@email.com");

    mvc.perform(get("/api/v1/reports/dashboard").header(HttpHeaders.AUTHORIZATION, student))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mvc.perform(get("/api/v1/reports/dashboard").header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/admin/outbox-events").header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/outbox-events").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk());
    mvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, student))
        .andExpect(status().isForbidden());
  }

  @Test
  void publicRegistrationCannotEscalateItsRole() throws Exception {
    var email = "role-injection-" + UUID.randomUUID() + "@example.com";
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"SecurePassword!2026","roles":["ADMIN"]}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.roles").doesNotExist());

    var saved = users.findByEmail(email);
    assertEquals(1, saved.getAuthorities().size());
    assertEquals("ROLE_STUDENT", saved.getAuthorities().iterator().next().getAuthority());
    users.delete(saved);
  }

  @Test
  void loginFailureDoesNotRevealWhetherAnAccountExists() throws Exception {
    var existing = failedLogin("student2@email.com");
    var missing = failedLogin("missing-" + UUID.randomUUID() + "@example.com");

    assertEquals(existing.path("status").asInt(), missing.path("status").asInt());
    assertEquals(existing.path("code").asText(), missing.path("code").asText());
    assertEquals(existing.path("detail").asText(), missing.path("detail").asText());
  }

  @Test
  void sanitizesCorrelationIdsAndEmitsTheCompleteSecurityHeaderSet() throws Exception {
    var unsafe = "<script>alert(1)</script>";
    var response =
        mvc.perform(get("/actuator/health").header("X-Correlation-ID", unsafe))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(
                header()
                    .string(
                        "Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=()"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andReturn()
            .getResponse();

    var actual = response.getHeader("X-Correlation-ID");
    assertNotEquals(unsafe, actual);
    assertTrue(actual != null && actual.matches("[0-9a-f-]{36}"));

    mvc.perform(get("/actuator/health").header("X-Correlation-ID", "safe-correlation-123"))
        .andExpect(header().string("X-Correlation-ID", "safe-correlation-123"));

    mvc.perform(get("/actuator/health").secure(true))
        .andExpect(
            header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
  }

  private JsonNode failedLogin(String email) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, "definitely-wrong")))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString());
  }

  private String bearer(String email) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, "123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return "Bearer "
        + objectMapper.readTree(response.getContentAsString()).get("accessToken").asText();
  }

  private String credentials(String email, String password) {
    return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
  }
}
