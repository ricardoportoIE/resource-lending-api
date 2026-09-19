package com.ricardoporto.lending;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ricardoporto.lending.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ResourceLendingApplicationTests extends PostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {}

  @Test
  void runsAllFlywayMigrationsOnACleanDatabase() {
    var successfulMigrations =
        jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success", Integer.class);
    var domainTables =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name in (
                'usuarios', 'refresh_tokens', 'resources', 'resource_items', 'loans',
                'loan_policies', 'audit_events', 'reservations'
              )
            """,
            Integer.class);
    var preservedLegacyTables =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'legacy'
              and table_name in (
                'cliente', 'aluno', 'pai_de_aluno', 'exemplar', 'livro', 'artigo',
                'periodico', 'emprestimo'
              )
            """,
            Integer.class);
    var requiredIndexes =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from pg_indexes
            where schemaname = 'public'
              and indexname in (
                'idx_usuarios_perfis_perfis_id',
                'idx_refresh_tokens_usuario_active',
                'idx_resources_type',
                'idx_resources_category',
                'idx_resource_items_resource_status',
                'idx_loans_borrower_status',
                'idx_loans_item_status',
                'idx_loans_due_at',
                'idx_audit_events_entity',
                'idx_audit_events_occurred_at',
                'idx_reservations_resource_queue',
                'idx_reservations_user_status',
                'uk_reservations_user_resource_open',
                'uk_loans_item_open'
              )
            """,
            Integer.class);
    var configuredRoles =
        jdbcTemplate.queryForObject(
            "select count(*) from perfis where nome in ('ROLE_STUDENT', 'ROLE_STAFF', 'ROLE_ADMIN')",
            Integer.class);
    var legacyRoles =
        jdbcTemplate.queryForObject(
            "select count(*) from perfis where nome = 'ROLE_USER'", Integer.class);

    assertEquals(7, successfulMigrations);
    assertEquals(8, domainTables);
    assertEquals(8, preservedLegacyTables);
    assertEquals(14, requiredIndexes);
    assertEquals(3, configuredRoles);
    assertEquals(0, legacyRoles);
  }
}
