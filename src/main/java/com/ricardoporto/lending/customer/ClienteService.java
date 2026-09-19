package com.ricardoporto.lending.customer;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import com.ricardoporto.lending.shared.security.AuthorizationService;
import com.ricardoporto.lending.user.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
  private final ClienteRepository clienteRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuthorizationService authorizationService;

  public ClienteService(
      ClienteRepository clienteRepository,
      UsuarioRepository usuarioRepository,
      AuthorizationService authorizationService) {
    this.clienteRepository = clienteRepository;
    this.usuarioRepository = usuarioRepository;
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findAll() {
    var customers =
        authorizationService.isStaffOrAdmin()
            ? clienteRepository.findAll()
            : clienteRepository.findAllByUsuarioEmail(
                authorizationService.currentUser().getEmail());
    return customers.stream().map(ClienteMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public ClienteDto findById(Long codigo) {
    var customer = requireCliente(codigo);
    assertCanRead(customer);
    return ClienteMapper.toDto(customer);
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findByName(String nome) {
    var customers =
        authorizationService.isStaffOrAdmin()
            ? clienteRepository.findByNomeContainingIgnoreCase(nome)
            : clienteRepository.findByNomeContainingIgnoreCaseAndUsuarioEmail(
                nome, authorizationService.currentUser().getEmail());
    return customers.stream().map(ClienteMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findByType(String tipo) {
    return findAll().stream().filter(customer -> customer.tipo().equalsIgnoreCase(tipo)).toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ClienteDto create(ClientePostDto request) {
    Cliente cliente = createSubtype(request.tipo());
    cliente.setNome(request.nome());
    cliente.setIdade(request.idade());
    cliente.setTelefone(request.telefone());
    cliente.setEndereco(request.endereco());
    if (request.ownerUserId() != null) {
      cliente.setUsuario(
          usuarioRepository
              .findById(request.ownerUserId())
              .orElseThrow(() -> new ResourceNotFoundException("User", request.ownerUserId())));
    }
    return ClienteMapper.toDto(clienteRepository.save(cliente));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ClienteDto update(Long codigo, ClientePatchDto request) {
    var cliente = requireCliente(codigo);
    if (request.nome() != null) cliente.setNome(request.nome());
    if (request.idade() != null) cliente.setIdade(request.idade());
    if (request.telefone() != null) cliente.setTelefone(request.telefone());
    if (request.endereco() != null) cliente.setEndereco(request.endereco());
    return ClienteMapper.toDto(clienteRepository.save(cliente));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public void delete(Long codigo) {
    clienteRepository.delete(requireCliente(codigo));
  }

  private void assertCanRead(Cliente customer) {
    if (authorizationService.isStaffOrAdmin()) return;
    var owner = customer.getUsuario();
    if (owner == null || !owner.getId().equals(authorizationService.currentUser().getId())) {
      throw new AccessDeniedException("Customer does not belong to the authenticated user.");
    }
  }

  private Cliente requireCliente(Long codigo) {
    return clienteRepository
        .findById(codigo)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", codigo));
  }

  private Cliente createSubtype(String type) {
    if ("ALUNO".equalsIgnoreCase(type)) return new Aluno();
    if ("PAI_DE_ALUNO".equalsIgnoreCase(type)) return new PaiDeAluno();
    throw new ApiException(
        HttpStatus.BAD_REQUEST, "INVALID_CUSTOMER_TYPE", "Invalid customer type.");
  }
}
