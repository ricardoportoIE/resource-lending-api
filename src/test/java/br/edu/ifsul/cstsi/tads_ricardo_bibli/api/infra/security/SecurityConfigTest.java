package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.infra.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.support.MariaDbIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/*
   Se você nunca ouviu falar, Mocking é uma forma de teste onde, em vez de verificar os resultados, verificamos os
   métodos invocados.
*/

@SpringBootTest // Carrega o Context do app em um container Spring Boot
@AutoConfigureMockMvc // Autoconfigura o Spring Boot Web, modo Mockado (o que siginifica nos
// entregar um container com um Servlet, mas sem o servidor web)
@ActiveProfiles("test")
class SecurityConfigTest extends MariaDbIntegrationTest {

  @Autowired // @AutoConfigureMockMvc (anotação na linha 16) nos permite injetar esse Bean
  private MockMvc
      mvc; // Elimina a necessidade de um servidor e nos permite realizar chamadas "Mockadas" nos

  // end-points

  @Test
  void endpointLoginQuandoUsuarioExistenteESenhaCorretaEspera200Ok() throws Exception {
    // ARRANGE
    var json =
        """
            {
              "email": "admin@email.com",
              "senha": "123"
            }
            """;
    var url = "/api/v1/login";

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .content(json) // o body da requisição
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(200, response.getStatus());
  }

  @Test
  void endpointLoginQuandoUsuarioInexistenteEspera400BadRequest() throws Exception {
    // ARRANGE
    var json =
        """
            {
              "email": "admin",
              "senha": "123"
            }
            """;
    var url = "/api/v1/login";

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .content(json) // o body da requisição
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(400, response.getStatus());
  }

  @Test
  void endpointLoginQuandoUsuarioExistenteESenhaIncorretaEspera403() throws Exception {
    // ARRANGE
    var json =
        """
            {
              "email": "admin@email.com",
              "senha": "123456"
            }
            """;
    var url = "/api/v1/login";

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .content(json) // o body da requisição
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());
  }

  @Test
  void endpointCadastrarQuandoVerboPostComBodyIncorretoEspera400BadRequest() throws Exception {
    // ARRANGE
    var json =
        """
            {
              "email": "adminemail",
              "senha": "123"
            }
            """;
    var url = "/api/v1/usuarios/cadastrar";

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .content(json) // o body da requisição
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(400, response.getStatus());
  }

  @Test
  void endpointCadastrarQuandoVerboGetPutEDeleteEspera403Forbidden() throws Exception {
    // ARRANGE
    var json =
        """
            {
              "email": "admin@email.com",
              "senha": "123"
            }
            """;
    var url = "/api/v1/usuarios/cadastrar";

    // ACT + ASSERT
    this.mvc.perform(get(url)).andExpect(status().isForbidden());

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                put(url) // verbo na rota
                    .content(json) // o body da requisição
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());

    // ACT
    response =
        mvc.perform( // performa uma requisição
                delete(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());
  }

  @Test
  void endPointConfirmarEmailQuandoVerboGetETokenValidoEspera200Ok() throws Exception {
    // ARRANGE
    var url = "/confirmar-email?token=" + confirmationToken();

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                get(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(200, response.getStatus());
  }

  @Test
  void endPointConfirmarEmailQuandoVerboGetETokenInvalidoEspera400BadRequest() throws Exception {
    // ARRANGE
    var url = "/confirmar-email?token=token_invalido";

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                get(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(400, response.getStatus());
  }

  @Test
  void endPointConfirmarEmailQuandoVerboPostPutEDeleteETokenValidoEspera403Forbidden()
      throws Exception {
    // ARRANGE
    var url = "/confirmar-email?token=" + confirmationToken();

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());

    // ACT
    response =
        mvc.perform( // performa uma requisição
                put(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());

    // ACT
    response =
        mvc.perform( // performa uma requisição
                delete(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());
  }

  @Test
  void endPointProdutosQuandoVerboGetPostPutDeleteENaoEstaAutenticadoEspera403Forbidden()
      throws Exception {
    // ARRANGE
    var url = "/api/v1/produtos";
    var body =
        """
            {
              "key": "value",
              "key2": "value2"
            }
            """;

    // ACT + ASSERT
    this.mvc
        .perform(get(url)) // verbo na rota
        .andExpect(status().isForbidden());

    // ACT
    var response =
        mvc.perform( // performa uma requisição
                post(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());

    // ACT
    response =
        mvc.perform( // performa uma requisição
                put(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());

    // ACT
    response =
        mvc.perform( // performa uma requisição
                delete(url) // verbo na rota
                    .contentType(MediaType.APPLICATION_JSON) // o header Content-type
                )
            .andReturn()
            .getResponse(); // a response da requisição

    // ASSERT
    Assertions.assertEquals(403, response.getStatus());
  }
}
