package com.ricardoporto.lending.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import com.ricardoporto.lending.auth.AutenticacaoService;
import com.ricardoporto.lending.shared.security.TokenService;
import com.ricardoporto.lending.user.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiIntegrationTest extends PostgresIntegrationTest {
  @Autowired protected TestRestTemplate rest;
  @Autowired private AutenticacaoService authenticationService;
  @Autowired private TokenService tokenService;

  private String accessToken;

  @BeforeEach
  void authenticateAdmin() {
    var user = (Usuario) authenticationService.loadUserByUsername("admin@email.com");
    accessToken = tokenService.generateAccessToken(user);
    assertNotNull(accessToken);
  }

  protected HttpHeaders headers() {
    var headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
    return headers;
  }

  protected <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
    return exchange(url, POST, body, responseType);
  }

  protected <T> ResponseEntity<T> patch(String url, Object body, Class<T> responseType) {
    return exchange(url, PATCH, body, responseType);
  }

  protected <T> ResponseEntity<T> get(String url, Class<T> responseType) {
    return exchange(url, GET, null, responseType);
  }

  protected <T> ResponseEntity<T> delete(String url, Class<T> responseType) {
    return exchange(url, DELETE, null, responseType);
  }

  private <T> ResponseEntity<T> exchange(
      String url, HttpMethod method, Object body, Class<T> responseType) {
    return rest.exchange(url, method, new HttpEntity<>(body, headers()), responseType);
  }
}
