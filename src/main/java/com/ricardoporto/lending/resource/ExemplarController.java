package com.ricardoporto.lending.resource;

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
@RequestMapping("/api/v1/exemplares")
public class ExemplarController {

  private final ExemplarService exemplarService;

  public ExemplarController(ExemplarService exemplarService) {
    this.exemplarService = exemplarService;
  }

  @GetMapping
  public ResponseEntity<List<ExemplarDto>> getExemplares() {
    return ResponseEntity.ok(exemplarService.findAll());
  }

  @GetMapping("/{codigo}")
  public ResponseEntity<ExemplarDto> getExemplarById(@PathVariable Long codigo) {
    return ResponseEntity.ok(exemplarService.findById(codigo));
  }

  @GetMapping("/tipo/{tipo}")
  public ResponseEntity<List<ExemplarDto>> getExemplaresByTipo(@PathVariable String tipo) {
    return ResponseEntity.ok(exemplarService.findByType(tipo));
  }

  @PatchMapping("/{codigo}")
  public ResponseEntity<ExemplarDto> updateParcial(
      @PathVariable Long codigo, @Valid @RequestBody ExemplarPatchDto request) {
    return ResponseEntity.ok(exemplarService.update(codigo, request));
  }

  @PostMapping
  public ResponseEntity<ExemplarDto> create(@Valid @RequestBody ExemplarPostDto request) {
    var created = exemplarService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/exemplares/" + created.codigo()))
        .body(created);
  }

  @DeleteMapping("/{codigo}")
  public ResponseEntity<Void> delete(@PathVariable Long codigo) {
    exemplarService.delete(codigo);
    return ResponseEntity.noContent().build();
  }
}
