package com.ricardoporto.lending.support;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Shared ephemeral PostgreSQL database for integration tests. */
public abstract class PostgresIntegrationTest {

  private static final PostgreSQLContainer DATABASE =
      new PostgreSQLContainer("postgres:17.6-alpine").withDatabaseName("resource_lending_test");

  private static final String JWT_SECRET = UUID.randomUUID() + UUID.randomUUID().toString();

  static {
    DATABASE.start();
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
    registry.add("api.security.token.secret", () -> JWT_SECRET);
  }
}
