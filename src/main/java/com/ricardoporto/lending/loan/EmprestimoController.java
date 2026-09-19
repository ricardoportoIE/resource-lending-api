package com.ricardoporto.lending.loan;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emprestimos")
public class EmprestimoController {

  private final EmprestimoService emprestimoService;

  public EmprestimoController(EmprestimoService emprestimoService) {
    this.emprestimoService = emprestimoService;
  }

  @GetMapping
  public ResponseEntity<List<EmprestimoDto>> getEmprestimos() {
    return ResponseEntity.ok(emprestimoService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<EmprestimoDto> getEmprestimoById(@PathVariable Long id) {
    return ResponseEntity.ok(emprestimoService.findById(id));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<EmprestimoDto> updateParcial(
      @PathVariable Long id, @Valid @RequestBody EmprestimoPatchDto request) {
    return ResponseEntity.ok(emprestimoService.update(id, request));
  }

  @PostMapping
  public ResponseEntity<EmprestimoDto> create(@Valid @RequestBody EmprestimoPostDto request) {
    var created = emprestimoService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/emprestimos/" + created.id())).body(created);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    emprestimoService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
