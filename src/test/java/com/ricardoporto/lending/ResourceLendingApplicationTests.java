package com.ricardoporto.lending;

import com.ricardoporto.lending.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Este é um teste de fumaça (smoke test) fundamental. Seu único objetivo é verificar se o contexto
 * da aplicação Spring (ApplicationContext) pode ser carregado com sucesso, sem lançar exceções.
 */
@SpringBootTest // Anotação que carrega o contexto completo da aplicação Spring Boot para o teste
@ActiveProfiles("test") // Ativa o perfil de teste, para carregar o application-test.properties
class ResourceLendingApplicationTests extends PostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * Este método de teste verifica se o contexto da aplicação (ApplicationContext) é carregado com
   * sucesso. O corpo deste método pode ser vazio. O teste passa se a aplicação Spring Boot iniciar
   * e carregar todos os seus beans e configurações sem lançar uma exceção. Se houver um problema de
   * configuração, injeção de dependência, ou qualquer outro erro durante a inicialização, este
   * teste falhará.
   */
  @Test
  void contextLoads() {}

  @Test
  void shouldRunFlywayMigrationsOnCleanDatabase() {
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
                'cliente', 'exemplar', 'emprestimo', 'usuarios', 'refresh_tokens',
                'resources', 'resource_items', 'loans', 'loan_policies', 'audit_events'
                , 'reservations'
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
                'idx_emprestimo_cliente_id',
                'idx_emprestimo_exemplar_id',
                'idx_usuarios_perfis_perfis_id',
                'idx_refresh_tokens_usuario_active',
                'idx_cliente_usuario_id',
                'idx_resources_type',
                'idx_resources_category',
                'idx_resource_items_resource_status',
                'idx_loans_borrower_status',
                'idx_loans_item_status',
                'idx_loans_due_at',
                'idx_audit_events_entity',
                'idx_audit_events_occurred_at'
                , 'idx_reservations_resource_queue'
                , 'idx_reservations_user_status'
                , 'uk_reservations_user_resource_open'
                , 'uk_loans_item_open'
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

    Assertions.assertEquals(6, successfulMigrations);
    Assertions.assertEquals(11, domainTables);
    Assertions.assertEquals(17, requiredIndexes);
    Assertions.assertEquals(3, configuredRoles);
    Assertions.assertEquals(0, legacyRoles);
  }
}
