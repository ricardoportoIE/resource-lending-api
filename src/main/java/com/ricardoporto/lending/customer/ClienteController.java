package com.ricardoporto.lending.customer;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

  private final ClienteService clienteService;

  public ClienteController(ClienteService clienteService) {
    this.clienteService = clienteService;
  }

  @GetMapping
  public ResponseEntity<List<ClienteDto>> getClientes() {
    return ResponseEntity.ok(clienteService.findAll());
  }

  @GetMapping("/{codigo}")
  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  public ResponseEntity<ClienteDto> getClienteById(@PathVariable Long codigo) {
    return ResponseEntity.ok(clienteService.findById(codigo));
  }

  @GetMapping("/nome/{nome}")
  public ResponseEntity<List<ClienteDto>> getClientesByNome(@PathVariable String nome) {
    return ResponseEntity.ok(clienteService.findByName(nome));
  }

  @GetMapping("/tipo/{tipo}")
  public ResponseEntity<List<ClienteDto>> getClientesByTipo(@PathVariable String tipo) {
    return ResponseEntity.ok(clienteService.findByType(tipo));
  }

  @PatchMapping("/{codigo}")
  public ResponseEntity<ClienteDto> updateParcial(
      @PathVariable Long codigo, @Valid @RequestBody ClientePatchDto request) {
    return ResponseEntity.ok(clienteService.update(codigo, request));
  }

  @PostMapping
  public ResponseEntity<ClienteDto> create(@Valid @RequestBody ClientePostDto request) {
    var created = clienteService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/clientes/" + created.codigo())).body(created);
  }

  @DeleteMapping("/{codigo}")
  public ResponseEntity<Void> delete(@PathVariable Long codigo) {
    clienteService.delete(codigo);
    return ResponseEntity.noContent().build();
  }
}
