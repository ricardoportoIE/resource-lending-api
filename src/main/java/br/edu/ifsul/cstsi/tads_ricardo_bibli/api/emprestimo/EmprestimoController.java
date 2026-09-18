package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.emprestimo;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente.ClienteRepository;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar.ExemplarRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api/v1/emprestimos")
public class EmprestimoController {

  private final EmprestimoRepository emprestimoRepository;
  private final ClienteRepository clienteRepository;
  private final ExemplarRepository exemplarRepository;

  public EmprestimoController(
      EmprestimoRepository emprestimoRepository,
      ClienteRepository clienteRepository,
      ExemplarRepository exemplarRepository) {
    this.emprestimoRepository = emprestimoRepository;
    this.clienteRepository = clienteRepository;
    this.exemplarRepository = exemplarRepository;
  }

  @GetMapping
  public ResponseEntity<List<EmprestimoDto>> getEmprestimos() {
    var emprestimos = emprestimoRepository.findAll();
    return ResponseEntity.ok().body(emprestimos.stream().map(EmprestimoDto::new).toList());
  }

  @GetMapping("{id}")
  public ResponseEntity<?> getEmprestimoById(@PathVariable("id") Long id) {
    var emprestimo = emprestimoRepository.findById(id);

    if (emprestimo.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Empréstimo com ID " + id + " não encontrado.");
    }

    return ResponseEntity.ok(new EmprestimoDto(emprestimo.get()));
  }

  // PATCH
  @PatchMapping("/{id}")
  public ResponseEntity<EmprestimoDto> updateParcial(
      @PathVariable Long id, @RequestBody @Valid EmprestimoPatchDto dto) {
    return emprestimoRepository
        .findById(id)
        .map(
            emprestimo -> {
              if (dto.dataEmprestimo() != null) emprestimo.setDataEmprestimo(dto.dataEmprestimo());
              if (dto.dataDevolucao() != null) emprestimo.setDataDevolucao(dto.dataDevolucao());

              if (dto.clienteId() != null) {
                var cliente =
                    clienteRepository
                        .findById(dto.clienteId())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Cliente com código " + dto.clienteId() + " não encontrado."));
                emprestimo.setCliente(cliente);
              }

              if (dto.exemplarId() != null) {
                var exemplar =
                    exemplarRepository
                        .findById(dto.exemplarId())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Exemplar com código "
                                        + dto.exemplarId()
                                        + " não encontrado."));
                emprestimo.setExemplar(exemplar);
              }

              emprestimoRepository.save(emprestimo);
              return ResponseEntity.ok(new EmprestimoDto(emprestimo));
            })
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Empréstimo com ID " + id + " não encontrado."));
  }

  // POST
  @PostMapping
  public ResponseEntity<EmprestimoDto> create(@Valid @RequestBody EmprestimoPostDto dto) {
    var cliente =
        clienteRepository
            .findById(dto.clienteId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente com código " + dto.clienteId() + " não encontrado."));

    var exemplar =
        exemplarRepository
            .findById(dto.exemplarId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exemplar com código " + dto.exemplarId() + " não encontrado."));

    var emprestimo = new Emprestimo();
    emprestimo.setDataEmprestimo(
        dto.dataEmprestimo() != null ? dto.dataEmprestimo() : LocalDate.now());
    emprestimo.setDataDevolucao(dto.dataDevolucao());
    emprestimo.setCliente(cliente);
    emprestimo.setExemplar(exemplar);

    var saved = emprestimoRepository.save(emprestimo);
    return ResponseEntity.status(HttpStatus.CREATED).body(new EmprestimoDto(saved));
  }

  // DELETE
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    return emprestimoRepository
        .findById(id)
        .map(
            emprestimo -> {
              emprestimoRepository.delete(emprestimo);
              return ResponseEntity.noContent().build();
            })
        .orElse(
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Empréstimo com ID " + id + " não encontrado."));
  }
}
