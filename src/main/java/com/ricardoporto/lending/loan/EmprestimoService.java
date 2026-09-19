package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.customer.Cliente;
import com.ricardoporto.lending.customer.ClienteRepository;
import com.ricardoporto.lending.resource.Exemplar;
import com.ricardoporto.lending.resource.ExemplarRepository;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmprestimoService {

  private final EmprestimoRepository emprestimoRepository;
  private final ClienteRepository clienteRepository;
  private final ExemplarRepository exemplarRepository;

  public EmprestimoService(
      EmprestimoRepository emprestimoRepository,
      ClienteRepository clienteRepository,
      ExemplarRepository exemplarRepository) {
    this.emprestimoRepository = emprestimoRepository;
    this.clienteRepository = clienteRepository;
    this.exemplarRepository = exemplarRepository;
  }

  @Transactional(readOnly = true)
  public List<EmprestimoDto> findAll() {
    return emprestimoRepository.findAll().stream().map(EmprestimoMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public EmprestimoDto findById(Long id) {
    return EmprestimoMapper.toDto(requireEmprestimo(id));
  }

  @Transactional
  public EmprestimoDto create(EmprestimoPostDto request) {
    var emprestimo = new Emprestimo();
    emprestimo.setDataEmprestimo(
        request.dataEmprestimo() == null ? LocalDate.now() : request.dataEmprestimo());
    emprestimo.setDataDevolucao(request.dataDevolucao());
    emprestimo.setCliente(requireCliente(request.clienteId()));
    emprestimo.setExemplar(requireExemplar(request.exemplarId()));
    return EmprestimoMapper.toDto(emprestimoRepository.save(emprestimo));
  }

  @Transactional
  public EmprestimoDto update(Long id, EmprestimoPatchDto request) {
    var emprestimo = requireEmprestimo(id);
    if (request.dataEmprestimo() != null) emprestimo.setDataEmprestimo(request.dataEmprestimo());
    if (request.dataDevolucao() != null) emprestimo.setDataDevolucao(request.dataDevolucao());
    if (request.clienteId() != null) emprestimo.setCliente(requireCliente(request.clienteId()));
    if (request.exemplarId() != null) emprestimo.setExemplar(requireExemplar(request.exemplarId()));
    return EmprestimoMapper.toDto(emprestimoRepository.save(emprestimo));
  }

  @Transactional
  public void delete(Long id) {
    emprestimoRepository.delete(requireEmprestimo(id));
  }

  private Emprestimo requireEmprestimo(Long id) {
    return emprestimoRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
  }

  private Cliente requireCliente(Long id) {
    return clienteRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
  }

  private Exemplar requireExemplar(Long id) {
    return exemplarRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
  }
}
