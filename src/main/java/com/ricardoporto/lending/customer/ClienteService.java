package com.ricardoporto.lending.customer;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

  private final ClienteRepository clienteRepository;

  public ClienteService(ClienteRepository clienteRepository) {
    this.clienteRepository = clienteRepository;
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findAll() {
    return clienteRepository.findAll().stream().map(ClienteMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public ClienteDto findById(Long codigo) {
    return ClienteMapper.toDto(requireCliente(codigo));
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findByName(String nome) {
    return clienteRepository.findByNomeContainingIgnoreCase(nome).stream()
        .map(ClienteMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClienteDto> findByType(String tipo) {
    return clienteRepository.findAll().stream()
        .filter(cliente -> cliente.getClass().getSimpleName().equalsIgnoreCase(tipo))
        .map(ClienteMapper::toDto)
        .toList();
  }

  @Transactional
  public ClienteDto create(ClientePostDto request) {
    Cliente cliente = createSubtype(request.tipo());
    cliente.setNome(request.nome());
    cliente.setIdade(request.idade());
    cliente.setTelefone(request.telefone());
    cliente.setEndereco(request.endereco());
    return ClienteMapper.toDto(clienteRepository.save(cliente));
  }

  @Transactional
  public ClienteDto update(Long codigo, ClientePatchDto request) {
    var cliente = requireCliente(codigo);
    if (request.nome() != null) cliente.setNome(request.nome());
    if (request.idade() != null) cliente.setIdade(request.idade());
    if (request.telefone() != null) cliente.setTelefone(request.telefone());
    if (request.endereco() != null) cliente.setEndereco(request.endereco());
    return ClienteMapper.toDto(clienteRepository.save(cliente));
  }

  @Transactional
  public void delete(Long codigo) {
    clienteRepository.delete(requireCliente(codigo));
  }

  private Cliente requireCliente(Long codigo) {
    return clienteRepository
        .findById(codigo)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", codigo));
  }

  private Cliente createSubtype(String type) {
    if ("ALUNO".equalsIgnoreCase(type)) {
      return new Aluno();
    }
    if ("PAI_DE_ALUNO".equalsIgnoreCase(type)) {
      return new PaiDeAluno();
    }
    throw new ApiException(
        HttpStatus.BAD_REQUEST, "INVALID_CUSTOMER_TYPE", "Invalid customer type.");
  }
}
