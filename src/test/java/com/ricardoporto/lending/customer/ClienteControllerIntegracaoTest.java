package com.ricardoporto.lending.customer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class ClienteControllerIntegracaoTest extends BaseAPIIntegracaoTest {
  private ResponseEntity<ClienteDto> getCliente(String url) {
    return get(url, ClienteDto.class);
  }

  private ResponseEntity<List<ClienteDto>> getClientesList(String url) {
    var headers = getHeaders();

    return rest.exchange(
        url, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});
  }

  @Test
  public void testPatchClienteEspera200OkEDelete204NotContent() {
    // ARRANGE
    var clientePostDto =
        new ClientePostDto("Cliente Teste", 30, "1234567890", "Rua A, n. 100", "ALUNO");

    var responsePost = post("/api/v1/clientes", clientePostDto, null);
    assertEquals(HttpStatus.CREATED, responsePost.getStatusCode());

    var location = responsePost.getHeaders().get("location").get(0);
    var clienteDto = getCliente(location).getBody();

    assertNotNull(clienteDto);
    assertEquals("Cliente Teste", clienteDto.nome());
    assertEquals(30, clienteDto.idade());
    assertEquals("1234567890", clienteDto.telefone());
    assertEquals("Rua A, n. 100", clienteDto.endereco());

    // ‘PATCH’: atualiza apenas nome e telefone
    var clientePatchDto = new ClientePatchDto("Cliente Atualizado", null, "0987654321", null);

    // ACT
    var responsePatch = patch(location, clientePatchDto, ClienteDto.class);

    // ASSERT
    assertEquals(HttpStatus.OK, responsePatch.getStatusCode());
    assertNotNull(responsePatch.getBody());
    assertEquals("Cliente Atualizado", responsePatch.getBody().nome());
    assertEquals(30, responsePatch.getBody().idade()); // idade permanece igual
    assertEquals("0987654321", responsePatch.getBody().telefone());
    assertEquals("Rua A, n. 100", responsePatch.getBody().endereco()); // endereço não alterado

    // ACT: Remoção
    var responseDelete = delete(location, null);
    // assert delete 204
    assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
  }

  @Test
  @DisplayName("Deve criar um novo cliente do tipo PAI_DE_ALUNO")
  public void testCreatePaiDeAluno() {
    // ARRANGE
    var clientePostDto =
        new ClientePostDto("Pai de Aluno Teste", 40, "5555555555", "Rua C, n. 300", "PAI_DE_ALUNO");

    // ACT
    var responsePost = post("/api/v1/clientes", clientePostDto, null);

    // ASSERT
    assertEquals(HttpStatus.CREATED, responsePost.getStatusCode());

    var location = responsePost.getHeaders().get("location").get(0);
    var clienteDto = getCliente(location).getBody();

    assertNotNull(clienteDto);
    assertEquals("Pai de Aluno Teste", clienteDto.nome());
    assertEquals(40, clienteDto.idade());
    assertEquals("5555555555", clienteDto.telefone());
    assertEquals("Rua C, n. 300", clienteDto.endereco());

    // Limpa o teste removendo o cliente criado
    delete(location, null);
  }

  @Test
  @DisplayName("Deve retornar lista vazia quando não houver clientes cadastrados")
  public void deveRetornarListaVaziaQuandoNaoHouverClientes() {
    // Arrange
    final String CLIENTES_API_URL = "/api/v1/clientes";
    final String MENSAGEM_LISTA_VAZIA = "A lista de clientes deve estar vazia";
    final String MENSAGEM_STATUS_OK = "O status da resposta deve ser OK";
    final String MENSAGEM_CORPO_NAO_NULO = "O corpo da resposta não deve ser nulo";

    // Act
    ResponseEntity<List<ClienteDto>> response = getClientesList(CLIENTES_API_URL);

    // Assert
    assertAll(
        () -> assertEquals(HttpStatus.OK, response.getStatusCode(), MENSAGEM_STATUS_OK),
        () -> assertNotNull(response.getBody(), MENSAGEM_CORPO_NAO_NULO),
        () -> assertTrue(response.getBody().isEmpty(), MENSAGEM_LISTA_VAZIA));
  }

  @Test
  @DisplayName("Deve retornar um cliente específico pelo ID")
  public void testGetClienteById() {
    // ARRANGE
    // Primeiro cria um cliente para garantir que existe um para buscar
    var clientePostDto =
        new ClientePostDto("Cliente Teste ID", 25, "9876543210", "Rua B, n. 200", "ALUNO");

    var responsePost = post("/api/v1/clientes", clientePostDto, null);
    assertEquals(HttpStatus.CREATED, responsePost.getStatusCode());

    var location = responsePost.getHeaders().get("location").get(0);

    // ACT
    var response = getCliente(location);

    // ASSERT
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Cliente Teste ID", response.getBody().nome());
    assertEquals(25, response.getBody().idade());
    assertEquals("9876543210", response.getBody().telefone());
    assertEquals("Rua B, n. 200", response.getBody().endereco());

    // Limpa o teste removendo o cliente criado
    delete(location, null);
  }
}
