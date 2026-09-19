package com.ricardoporto.lending.shared.security;

import com.ricardoporto.lending.user.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private final Map<String, Window> windows = new ConcurrentHashMap<>();
  private final SecurityProblemWriter problemWriter;
  private final int requestsPerWindow;
  private final int authRequestsPerWindow;
  private final Duration windowSize;
  private final Clock clock;

  @Autowired
  public RateLimitFilter(
      SecurityProblemWriter problemWriter,
      @Value("${api.security.rate-limit.requests-per-window}") int requestsPerWindow,
      @Value("${api.security.rate-limit.auth-requests-per-window}") int authRequestsPerWindow,
      @Value("${api.security.rate-limit.window}") Duration windowSize) {
    this(problemWriter, requestsPerWindow, authRequestsPerWindow, windowSize, Clock.systemUTC());
  }

  RateLimitFilter(
      SecurityProblemWriter problemWriter,
      int requestsPerWindow,
      int authRequestsPerWindow,
      Duration windowSize,
      Clock clock) {
    this.problemWriter = problemWriter;
    this.requestsPerWindow = requestsPerWindow;
    this.authRequestsPerWindow = authRequestsPerWindow;
    this.windowSize = windowSize;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var identity =
        authentication != null && authentication.getPrincipal() instanceof Usuario user
            ? "user:" + user.getId()
            : "ip:" + request.getRemoteAddr();
    var authCommand =
        request.getRequestURI().startsWith("/api/v1/auth/")
            && !"GET".equalsIgnoreCase(request.getMethod());
    var limit = authCommand ? authRequestsPerWindow : requestsPerWindow;
    var key = (authCommand ? "auth:" : "api:") + identity;
    var decision =
        windows
            .computeIfAbsent(key, ignored -> new Window(clock.instant(), 0))
            .consume(clock.instant(), windowSize, limit);

    response.setHeader("RateLimit-Limit", String.valueOf(limit));
    response.setHeader("RateLimit-Remaining", String.valueOf(decision.remaining()));
    response.setHeader("RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));
    if (!decision.allowed()) {
      response.setHeader(
          "Retry-After",
          String.valueOf(
              Math.max(1, decision.resetEpochSeconds() - clock.instant().getEpochSecond())));
      problemWriter.write(
          request,
          response,
          HttpStatus.TOO_MANY_REQUESTS,
          "RATE_LIMIT_EXCEEDED",
          "Too many requests. Retry after the current rate-limit window.");
      return;
    }
    filterChain.doFilter(request, response);
  }

  @Scheduled(fixedDelay = 600_000)
  public void removeExpiredWindows() {
    var cutoff = clock.instant().minus(windowSize);
    windows.entrySet().removeIf(entry -> entry.getValue().startedBefore(cutoff));
  }

  private static final class Window {
    private Instant startedAt;
    private int count;

    private Window(Instant startedAt, int count) {
      this.startedAt = startedAt;
      this.count = count;
    }

    synchronized Decision consume(Instant now, Duration size, int limit) {
      if (!now.isBefore(startedAt.plus(size))) {
        startedAt = now;
        count = 0;
      }
      var allowed = count < limit;
      if (allowed) count++;
      return new Decision(
          allowed, Math.max(0, limit - count), startedAt.plus(size).getEpochSecond());
    }

    synchronized boolean startedBefore(Instant cutoff) {
      return startedAt.isBefore(cutoff);
    }
  }

  private record Decision(boolean allowed, int remaining, long resetEpochSeconds) {}
}
