package com.ricardoporto.lending.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private TokenService tokenService;
  @Autowired private UsuarioRepository usuarios;

  @Test
  void returnsTheAuthenticatedProfileAndRoles() throws Exception {
    var admin = usuarios.findByEmail("admin@email.com");

    mvc.perform(
            get("/api/v1/users/me")
                .header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generateAccessToken(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@email.com"))
        .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
  }

  @Test
  void rejectsAnAnonymousProfileRequest() throws Exception {
    mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }
}
