package com.ricardoporto.lending.observability;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ricardoporto.lending.shared.logging.CorrelationIdFilter;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.support.PostgresIntegrationTest;
import com.ricardoporto.lending.user.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class ObservabilityIntegrationTest extends PostgresIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private TokenService tokenService;
  @Autowired private UsuarioRepository usuarioRepository;

  @Test
  void exposesHealthButProtectsOperationalMetrics() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/actuator/prometheus")
                .header(HttpHeaders.AUTHORIZATION, bearer("student1@email.com")))
        .andExpect(status().isForbidden());
    var metrics =
        mvc.perform(
                get("/actuator/prometheus")
                    .header(HttpHeaders.AUTHORIZATION, bearer("admin@email.com")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(metrics.contains("jvm_memory"));
  }

  @Test
  void propagatesSafeCorrelationIdIntoHeaderAndProblemDetail() throws Exception {
    var correlationId = "portfolio-request-123";
    mvc.perform(get("/api/v1/resources").header(CorrelationIdFilter.HEADER, correlationId))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CorrelationIdFilter.HEADER, correlationId))
        .andExpect(jsonPath("$.correlationId").value(correlationId));

    var generated =
        mvc.perform(get("/api/v1/resources").header(CorrelationIdFilter.HEADER, "unsafe value"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getHeader(CorrelationIdFilter.HEADER);
    assertNotEquals("unsafe value", generated);
  }

  @Test
  void publishesOpenApiForCurrentDomainEndpoints() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("Resource Lending API"))
        .andExpect(jsonPath("$.paths['/api/v1/resources']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/loans']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/reservations']").exists());
  }

  private String bearer(String email) {
    return "Bearer " + tokenService.generateAccessToken(usuarioRepository.findByEmail(email));
  }
}
