package com.ricardoporto.lending.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ricardoporto.lending.customer.Aluno;
import com.ricardoporto.lending.customer.ClienteRepository;
import com.ricardoporto.lending.customer.PaiDeAluno;
import com.ricardoporto.lending.loan.Emprestimo;
import com.ricardoporto.lending.loan.EmprestimoRepository;
import com.ricardoporto.lending.resource.Artigo;
import com.ricardoporto.lending.resource.ExemplarRepository;
import com.ricardoporto.lending.resource.Livro;
import com.ricardoporto.lending.resource.Periodico;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgreSqlPersistenceIntegrationTest extends PostgresIntegrationTest {

  @Autowired private ClienteRepository clienteRepository;
  @Autowired private ExemplarRepository exemplarRepository;
  @Autowired private EmprestimoRepository emprestimoRepository;

  @Test
  void shouldPersistAllLegacySubtypesWithMigratedSchema() {
    var aluno = new Aluno();
    aluno.setNome("Student fixture");
    aluno.setIdade(20);
    aluno.setTelefone("555-0100");
    aluno.setEndereco("Test address");

    var paiDeAluno = new PaiDeAluno();
    paiDeAluno.setNome("Parent fixture");
    paiDeAluno.setIdade(40);
    paiDeAluno.setTelefone("555-0101");
    paiDeAluno.setEndereco("Test address");

    var livro = new Livro();
    livro.setNome("Book fixture");
    livro.setAutor("Test author");
    livro.setEditora("Test publisher");
    livro.setEdicao(1);

    var artigo = new Artigo();
    artigo.setNome("Article fixture");
    artigo.setAutor("Test author");

    var periodico = new Periodico();
    periodico.setNome("Journal fixture");
    periodico.setEditora("Test publisher");

    clienteRepository.saveAndFlush(aluno);
    clienteRepository.saveAndFlush(paiDeAluno);
    exemplarRepository.saveAndFlush(livro);
    exemplarRepository.saveAndFlush(artigo);
    exemplarRepository.saveAndFlush(periodico);

    var emprestimo = new Emprestimo();
    emprestimo.setCliente(aluno);
    emprestimo.setExemplar(livro);
    emprestimo.setDataEmprestimo(LocalDate.now());
    emprestimo.setDataDevolucao(LocalDate.now().plusDays(14));
    emprestimoRepository.saveAndFlush(emprestimo);

    assertAll(
        () -> assertNotNull(aluno.getCodigo()),
        () -> assertNotNull(paiDeAluno.getCodigo()),
        () -> assertNotNull(livro.getCodigo()),
        () -> assertNotNull(artigo.getCodigo()),
        () -> assertNotNull(periodico.getCodigo()),
        () -> assertNotNull(emprestimo.getId()));
  }
}
