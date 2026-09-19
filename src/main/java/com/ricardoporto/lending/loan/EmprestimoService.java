package com.ricardoporto.lending.loan;

import com.ricardoporto.lending.customer.Cliente;
import com.ricardoporto.lending.customer.ClienteRepository;
import com.ricardoporto.lending.resource.Exemplar;
import com.ricardoporto.lending.resource.ExemplarRepository;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import com.ricardoporto.lending.shared.security.AuthorizationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmprestimoService {
  private final EmprestimoRepository emprestimoRepository;
  private final ClienteRepository clienteRepository;
  private final ExemplarRepository exemplarRepository;
  private final AuthorizationService authorizationService;

  public EmprestimoService(
      EmprestimoRepository emprestimoRepository,
      ClienteRepository clienteRepository,
      ExemplarRepository exemplarRepository,
      AuthorizationService authorizationService) {
    this.emprestimoRepository = emprestimoRepository;
    this.clienteRepository = clienteRepository;
    this.exemplarRepository = exemplarRepository;
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  public List<EmprestimoDto> findAll() {
    var loans =
        authorizationService.isStaffOrAdmin()
            ? emprestimoRepository.findAll()
            : emprestimoRepository.findAllByClienteUsuarioEmail(
                authorizationService.currentUser().getEmail());
    return loans.stream().map(EmprestimoMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public EmprestimoDto findById(Long id) {
    var loan = requireEmprestimo(id);
    assertCanAccess(loan.getCliente());
    return EmprestimoMapper.toDto(loan);
  }

  @Transactional
  public EmprestimoDto create(EmprestimoPostDto request) {
    var customer = requireCliente(request.clienteId());
    assertCanAccess(customer);
    var emprestimo = new Emprestimo();
    emprestimo.setDataEmprestimo(
        request.dataEmprestimo() == null ? LocalDate.now() : request.dataEmprestimo());
    emprestimo.setDataDevolucao(request.dataDevolucao());
    emprestimo.setCliente(customer);
    emprestimo.setExemplar(requireExemplar(request.exemplarId()));
    return EmprestimoMapper.toDto(emprestimoRepository.save(emprestimo));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public EmprestimoDto update(Long id, EmprestimoPatchDto request) {
    var emprestimo = requireEmprestimo(id);
    if (request.dataEmprestimo() != null) emprestimo.setDataEmprestimo(request.dataEmprestimo());
    if (request.dataDevolucao() != null) emprestimo.setDataDevolucao(request.dataDevolucao());
    if (request.clienteId() != null) emprestimo.setCliente(requireCliente(request.clienteId()));
    if (request.exemplarId() != null) emprestimo.setExemplar(requireExemplar(request.exemplarId()));
    return EmprestimoMapper.toDto(emprestimoRepository.save(emprestimo));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public void delete(Long id) {
    emprestimoRepository.delete(requireEmprestimo(id));
  }

  private void assertCanAccess(Cliente customer) {
    if (authorizationService.isStaffOrAdmin()) return;
    var owner = customer.getUsuario();
    if (owner == null || !owner.getId().equals(authorizationService.currentUser().getId())) {
      throw new AccessDeniedException("Loan does not belong to the authenticated user.");
    }
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
