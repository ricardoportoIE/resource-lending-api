package com.ricardoporto.lending.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
  private final SecurityProblemWriter problemWriter;

  public ApiAccessDeniedHandler(SecurityProblemWriter problemWriter) {
    this.problemWriter = problemWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    problemWriter.write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "ACCESS_DENIED",
        "You do not have permission to perform this operation.");
  }
}
