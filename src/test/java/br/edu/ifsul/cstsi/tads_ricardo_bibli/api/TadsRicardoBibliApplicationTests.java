package br.edu.ifsul.cstsi.tads_ricardo_bibli.api;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.support.MariaDbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Este é um teste de fumaça (smoke test) fundamental. Seu único objetivo é verificar se o contexto
 * da aplicação Spring (ApplicationContext) pode ser carregado com sucesso, sem lançar exceções.
 */
@SpringBootTest // Anotação que carrega o contexto completo da aplicação Spring Boot para o teste
@ActiveProfiles("test") // Ativa o perfil de teste, para carregar o application-test.properties
class TadsRicardoBibliApplicationTests extends MariaDbIntegrationTest {

  /**
   * Este método de teste verifica se o contexto da aplicação (ApplicationContext) é carregado com
   * sucesso. O corpo deste método pode ser vazio. O teste passa se a aplicação Spring Boot iniciar
   * e carregar todos os seus beans e configurações sem lançar uma exceção. Se houver um problema de
   * configuração, injeção de dependência, ou qualquer outro erro durante a inicialização, este
   * teste falhará.
   */
  @Test
  void contextLoads() {}
}
