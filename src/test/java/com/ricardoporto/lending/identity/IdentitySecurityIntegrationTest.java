package com.ricardoporto.lending.identity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentitySecurityIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UsuarioRepository users;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void registrationRequiresTheOneTimeEmailConfirmationToken() throws Exception {
    var email = "confirm-" + UUID.randomUUID() + "@example.com";
    register(email, "SecurePassword!2026");
    assertFalse(users.findByEmail(email).isEnabled());

    var token = latestOutboxToken(email, "EMAIL_CONFIRMATION_REQUESTED");
    mvc.perform(
            post("/api/v1/auth/email/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)))
        .andExpect(status().isNoContent());
    assertTrue(users.findByEmail(email).isEnabled());

    mvc.perform(
            post("/api/v1/auth/email/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_IDENTITY_TOKEN"));
  }

  @Test
  void passwordResetIsEnumerationSafeAndInvalidatesOldCredentials() throws Exception {
    mvc.perform(
            post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\"}"))
        .andExpect(status().isAccepted());

    var oldAccess = login("student1@email.com", "123", 200);
    mvc.perform(
            post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student1@email.com\"}"))
        .andExpect(status().isAccepted());
    var token = latestOutboxToken("student1@email.com", "PASSWORD_RESET_REQUESTED");

    mvc.perform(
            post("/api/v1/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"%s\",\"newPassword\":\"Replacement!2026\"}".formatted(token)))
        .andExpect(status().isNoContent());
    login("student1@email.com", "123", 401);
    login("student1@email.com", "Replacement!2026", 200);

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/resources")
                .header("Authorization", "Bearer " + oldAccess))
        .andExpect(status().isUnauthorized());

    jdbc.update(
        "update usuarios set senha = ?, security_version = security_version + 1 where email = ?",
        passwordEncoder.encode("123"),
        "student1@email.com");
  }

  @Test
  void failedLoginsProgressivelyLockTheAccount() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      login("student2@email.com", "wrong-password", 401);
    }
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("student2@email.com", "123")))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("ACCOUNT_TEMPORARILY_LOCKED"));

    jdbc.update(
        "update usuarios set failed_login_attempts = 0, locked_until = null where email = ?",
        "student2@email.com");
  }

  private void register(String email, String password) throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)))
        .andExpect(status().isCreated());
  }

  private String login(String email, String password, int statusCode) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, password)))
            .andExpect(status().is(statusCode))
            .andReturn()
            .getResponse();
    if (statusCode != 200) return null;
    return objectMapper.readTree(response.getContentAsString()).get("accessToken").asText();
  }

  private String latestOutboxToken(String email, String eventType) throws Exception {
    var payload =
        jdbc.queryForObject(
            "select payload from outbox_events where event_type = ? and payload like ? order by created_at desc limit 1",
            String.class,
            eventType,
            "%" + email + "%");
    return objectMapper.readTree(payload).get("token").asText();
  }

  private String credentials(String email, String password) {
    return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
  }
}
