package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.support;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;

/** Shared ephemeral database for integration tests in the current MariaDB-based architecture. */
public abstract class MariaDbIntegrationTest {

  private static final MariaDBContainer DATABASE =
      new MariaDBContainer("mariadb:11.4.5").withDatabaseName("resource_lending_test");

  private static final String JWT_SECRET = UUID.randomUUID() + UUID.randomUUID().toString();
  private static final String EMAIL_CONFIRMATION_TOKEN = UUID.randomUUID().toString();

  static {
    DATABASE.start();
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
    registry.add("api.security.token.secret", () -> JWT_SECRET);
    registry.add("api.security.email-confirmation-token", () -> EMAIL_CONFIRMATION_TOKEN);
  }

  protected static String confirmationToken() {
    return EMAIL_CONFIRMATION_TOKEN;
  }
}
