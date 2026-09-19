package com.ricardoporto.lending.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.user.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class IdempotencyFilter extends OncePerRequestFilter {
  public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
  public static final String IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";
  private static final Pattern LOAN_TRANSITION =
      Pattern.compile("^/api/v1/loans/[0-9a-fA-F-]+/(approve|collect|return)$");
  private static final Pattern RESERVATION_CREATE =
      Pattern.compile("^/api/v1/resources/[0-9a-fA-F-]+/reservations$");
  private static final Set<String> DIRECT_COMMANDS = Set.of("/api/v1/loans");
  private static final Pattern VALID_KEY = Pattern.compile("^[\\p{Graph}]{1,200}$");

  private final IdempotencyService service;
  private final ObjectMapper objectMapper;

  public IdempotencyFilter(IdempotencyService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())) return true;
    var path = request.getRequestURI();
    return !DIRECT_COMMANDS.contains(path)
        && !LOAN_TRANSITION.matcher(path).matches()
        && !RESERVATION_CREATE.matcher(path).matches();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var key = request.getHeader(IDEMPOTENCY_KEY);
    if (key == null || key.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }
    if (!VALID_KEY.matcher(key).matches()) {
      writeProblem(
          response,
          HttpStatus.BAD_REQUEST,
          "INVALID_IDEMPOTENCY_KEY",
          "Idempotency-Key must contain 1 to 200 visible characters.");
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Usuario user)) {
      filterChain.doFilter(request, response);
      return;
    }

    var cachedRequest = new CachedBodyHttpServletRequest(request);
    var claim =
        service.claim(key, user.getId(), request.getRequestURI(), hash(cachedRequest.body()));
    switch (claim.outcome()) {
      case REPLAY -> replay(response, claim.record());
      case CONFLICT ->
          writeProblem(
              response,
              HttpStatus.CONFLICT,
              "IDEMPOTENCY_KEY_REUSED",
              "The key was already used with a different request.");
      case IN_PROGRESS ->
          writeProblem(
              response,
              HttpStatus.CONFLICT,
              "IDEMPOTENCY_REQUEST_IN_PROGRESS",
              "A request with this key is still being processed.");
      case ACQUIRED -> execute(cachedRequest, response, filterChain, claim.record());
    }
  }

  private void execute(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain,
      IdempotencyRecord record)
      throws IOException, ServletException {
    var cachedResponse = new ContentCachingResponseWrapper(response);
    try {
      filterChain.doFilter(request, cachedResponse);
      var body = cachedResponse.getContentAsByteArray();
      service.complete(
          record.getId(),
          cachedResponse.getStatus(),
          new String(body, StandardCharsets.UTF_8),
          cachedResponse.getContentType(),
          cachedResponse.getHeader(HttpHeaders.LOCATION));
      cachedResponse.copyBodyToResponse();
    } catch (IOException | ServletException | RuntimeException exception) {
      service.release(record.getId());
      throw exception;
    }
  }

  private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
    response.setStatus(record.getHttpStatus());
    if (record.getContentType() != null) response.setContentType(record.getContentType());
    if (record.getLocation() != null)
      response.setHeader(HttpHeaders.LOCATION, record.getLocation());
    response.setHeader(IDEMPOTENCY_REPLAYED, "true");
    if (record.getResponseBody() != null) {
      response.getWriter().write(record.getResponseBody());
    }
  }

  private void writeProblem(
      HttpServletResponse response, HttpStatus status, String code, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        java.util.Map.of(
            "type",
            "https://resource-lending-api.dev/problems/" + code.toLowerCase().replace('_', '-'),
            "title",
            status.getReasonPhrase(),
            "status",
            status.value(),
            "detail",
            detail,
            "code",
            code));
  }

  private String hash(byte[] body) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
