package com.ricardoporto.lending.resource;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExemplarService {

  private final ExemplarRepository exemplarRepository;

  public ExemplarService(ExemplarRepository exemplarRepository) {
    this.exemplarRepository = exemplarRepository;
  }

  @Transactional(readOnly = true)
  public List<ExemplarDto> findAll() {
    return exemplarRepository.findAll().stream().map(ExemplarMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public ExemplarDto findById(Long codigo) {
    return ExemplarMapper.toDto(requireExemplar(codigo));
  }

  @Transactional(readOnly = true)
  public List<ExemplarDto> findByType(String tipo) {
    return exemplarRepository.findAll().stream()
        .filter(exemplar -> exemplar.getClass().getSimpleName().equalsIgnoreCase(tipo))
        .map(ExemplarMapper::toDto)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ExemplarDto create(ExemplarPostDto request) {
    var exemplar = createSubtype(request);
    exemplar.setNome(request.nome());
    return ExemplarMapper.toDto(exemplarRepository.save(exemplar));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ExemplarDto update(Long codigo, ExemplarPatchDto request) {
    var exemplar = requireExemplar(codigo);
    if (request.nome() != null) exemplar.setNome(request.nome());

    if (exemplar instanceof Livro livro) {
      if (request.autor() != null) livro.setAutor(request.autor());
      if (request.editora() != null) livro.setEditora(request.editora());
      if (request.edicao() != null) livro.setEdicao(request.edicao());
    } else if (exemplar instanceof Artigo artigo && request.autor() != null) {
      artigo.setAutor(request.autor());
    } else if (exemplar instanceof Periodico periodico && request.editora() != null) {
      periodico.setEditora(request.editora());
    }

    return ExemplarMapper.toDto(exemplarRepository.save(exemplar));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public void delete(Long codigo) {
    exemplarRepository.delete(requireExemplar(codigo));
  }

  private Exemplar requireExemplar(Long codigo) {
    return exemplarRepository
        .findById(codigo)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", codigo));
  }

  private Exemplar createSubtype(ExemplarPostDto request) {
    if ("LIVRO".equalsIgnoreCase(request.tipo())) {
      var livro = new Livro();
      livro.setAutor(request.autor());
      livro.setEditora(request.editora());
      livro.setEdicao(request.edicao());
      return livro;
    }
    if ("ARTIGO".equalsIgnoreCase(request.tipo())) {
      var artigo = new Artigo();
      artigo.setAutor(request.autor());
      return artigo;
    }
    if ("PERIODICO".equalsIgnoreCase(request.tipo())) {
      var periodico = new Periodico();
      periodico.setEditora(request.editora());
      return periodico;
    }
    throw new ApiException(
        HttpStatus.BAD_REQUEST, "INVALID_RESOURCE_TYPE", "Invalid resource type.");
  }
}
