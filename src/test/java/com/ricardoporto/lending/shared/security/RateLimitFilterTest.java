package com.ricardoporto.lending.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.user.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {
  @Test
  void rejectsRequestsBeyondTheIpWindowWithRetryHeaders() throws Exception {
    var writer = new SecurityProblemWriter(new ObjectMapper().findAndRegisterModules());
    var filter =
        new RateLimitFilter(
            writer,
            2,
            2,
            Duration.ofMinutes(1),
            Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC));

    execute(filter, 200);
    execute(filter, 200);
    var rejected = execute(filter, 429);
    assertEquals("0", rejected.getHeader("RateLimit-Remaining"));
    assertEquals("60", rejected.getHeader("Retry-After"));
  }

  @Test
  void isolatesAnonymousRequestWindowsByIpAddress() throws Exception {
    var filter = filter(1, 1);

    execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 200);
    execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 429);
    execute(filter, "GET", "/api/v1/resources", "192.0.2.11", 200);
  }

  @Test
  void appliesTheStricterAuthenticationCommandLimit() throws Exception {
    var filter = filter(5, 1);

    execute(filter, "POST", "/api/v1/auth/login", "192.0.2.10", 200);
    execute(filter, "POST", "/api/v1/auth/login", "192.0.2.10", 429);
    execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 200);
  }

  @Test
  void isolatesAuthenticatedWindowsByUserInsteadOfSharedIp() throws Exception {
    var filter = filter(1, 1);
    try {
      authenticate(10L);
      execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 200);
      execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 429);
      authenticate(11L);
      execute(filter, "GET", "/api/v1/resources", "192.0.2.10", 200);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private RateLimitFilter filter(int requestLimit, int authLimit) {
    return new RateLimitFilter(
        new SecurityProblemWriter(new ObjectMapper().findAndRegisterModules()),
        requestLimit,
        authLimit,
        Duration.ofMinutes(1),
        Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC));
  }

  private void authenticate(long id) {
    var user = new Usuario();
    user.setId(id);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
  }

  private MockHttpServletResponse execute(RateLimitFilter filter, int expectedStatus)
      throws Exception {
    return execute(filter, "GET", "/api/v1/resources", "192.0.2.10", expectedStatus);
  }

  private MockHttpServletResponse execute(
      RateLimitFilter filter, String method, String path, String address, int expectedStatus)
      throws Exception {
    var request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(address);
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    assertEquals(expectedStatus, response.getStatus());
    return response;
  }
}
