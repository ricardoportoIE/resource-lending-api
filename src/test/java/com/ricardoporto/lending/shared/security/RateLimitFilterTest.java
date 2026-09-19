package com.ricardoporto.lending.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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

  private MockHttpServletResponse execute(RateLimitFilter filter, int expectedStatus)
      throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/resources");
    request.setRemoteAddr("192.0.2.10");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    assertEquals(expectedStatus, response.getStatus());
    return response;
  }
}
