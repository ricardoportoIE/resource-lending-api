package com.ricardoporto.lending.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String PROBLEM_BASE = "https://resource-lending-api.dev/problems/";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              errors.put(fieldName, error.getDefaultMessage());
            });

    var problem =
        createProblem(
            HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request);
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetail> handleApiException(
      ApiException ex, HttpServletRequest request) {
    var problem = createProblem(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    return ResponseEntity.status(ex.getStatus()).body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleMalformedPayload(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    var problem =
        createProblem(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "The request body could not be read.",
            request);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleBadCredentialsException(
      BadCredentialsException ex, HttpServletRequest request) {
    var problem =
        createProblem(
            HttpStatus.FORBIDDEN,
            "INVALID_CREDENTIALS",
            "The supplied credentials are invalid.",
            request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {
    var problem =
        createProblem(
            ex.getStatusCode(),
            "HTTP_ERROR",
            ex.getReason() == null ? "The request could not be completed." : ex.getReason(),
            request);
    return ResponseEntity.status(ex.getStatusCode()).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {
    LOGGER.error("Unhandled request failure", ex);
    var problem =
        createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected internal error occurred.",
            request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private ProblemDetail createProblem(
      HttpStatusCode status, String code, String detail, HttpServletRequest request) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(PROBLEM_BASE + code.toLowerCase().replace('_', '-')));
    problem.setTitle(titleFor(status));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  private String titleFor(HttpStatusCode status) {
    var resolved = HttpStatus.resolve(status.value());
    return resolved == null ? "HTTP error" : resolved.getReasonPhrase();
  }
}
