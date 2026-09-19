package com.ricardoporto.lending.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void loginReturnsAccessAndRefreshTokens() throws Exception {
    var tokens = login("admin@email.com");

    assertNotNull(tokens.get("accessToken").textValue());
    assertNotNull(tokens.get("refreshToken").textValue());
    assertTrue(tokens.get("expiresIn").longValue() > 0);
    var rawRefreshToken = tokens.get("refreshToken").textValue();
    var plaintextMatches =
        jdbcTemplate.queryForObject(
            "select count(*) from refresh_tokens where token_hash = ?",
            Integer.class,
            rawRefreshToken);
    var hashedMatches =
        jdbcTemplate.queryForObject(
            "select count(*) from refresh_tokens where char_length(token_hash) = 64",
            Integer.class);
    assertEquals(0, plaintextMatches);
    assertTrue(hashedMatches > 0);
  }

  @Test
  void invalidCredentialsReturn401ProblemDetail() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("admin@email.com", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void protectedEndpointWithoutOrWithInvalidTokenReturns401() throws Exception {
    mvc.perform(get("/api/v1/exemplares"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    mvc.perform(get("/api/v1/exemplares").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  void studentCannotManageCatalogueButStaffCan() throws Exception {
    var resource =
        """
        {"name":"Clean Architecture","type":"BOOK","identifier":"SECURITY-TEST-BOOK","loanable":true}
        """;

    mvc.perform(
            post("/api/v1/resources")
                .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(resource))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    mvc.perform(
            post("/api/v1/resources")
                .header(HttpHeaders.AUTHORIZATION, bearer("staff@email.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(resource))
        .andExpect(status().isCreated());
  }

  @Test
  void registrationHashesPasswordAndAssignsStudentRole() throws Exception {
    var email = "new-student-" + UUID.randomUUID() + "@example.com";
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, "secure-password")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.senha").doesNotExist());

    var saved = usuarioRepository.findByEmail(email);
    assertNotNull(saved);
    assertTrue(saved.isEnabled());
    assertTrue(passwordEncoder.matches("secure-password", saved.getSenha()));
    assertTrue(
        saved.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT")));
    usuarioRepository.delete(saved);
  }

  @Test
  void refreshRotatesTokenAndReuseRevokesActiveTokens() throws Exception {
    var initial = login("student1@email.com");
    var firstRefresh = initial.get("refreshToken").textValue();
    var rotated = refresh(firstRefresh, 200);
    var secondRefresh = rotated.get("refreshToken").textValue();
    assertNotEquals(firstRefresh, secondRefresh);

    refresh(firstRefresh, 401);
    refresh(secondRefresh, 401);
  }

  @Test
  void logoutRevokesRefreshToken() throws Exception {
    var refreshToken = login("student2@email.com").get("refreshToken").textValue();
    mvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refreshToken)))
        .andExpect(status().isNoContent());

    refresh(refreshToken, 401);
  }

  @Test
  void studentOnlyReadsOwnedCustomerAndLoans() throws Exception {
    var authorization = bearer("student1@email.com");

    mvc.perform(get("/api/v1/clientes").header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].codigo").value(100));
    mvc.perform(get("/api/v1/clientes/101").header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mvc.perform(get("/api/v1/emprestimos").header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(100));
    mvc.perform(get("/api/v1/emprestimos/101").header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isForbidden());
  }

  private String bearer(String email) throws Exception {
    return "Bearer " + login(email).get("accessToken").textValue();
  }

  private JsonNode login(String email) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, "123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString());
  }

  private JsonNode refresh(String token, int expectedStatus) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshBody(token)))
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString());
  }

  private String credentials(String email, String password) {
    return """
        {"email":"%s","senha":"%s"}
        """
        .formatted(email, password);
  }

  private String refreshBody(String token) {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(token);
  }
}
