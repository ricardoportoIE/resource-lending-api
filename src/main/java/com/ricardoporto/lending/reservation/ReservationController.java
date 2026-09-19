package com.ricardoporto.lending.reservation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reservations", description = "FIFO reservation queue and pickup lifecycle")
public class ReservationController {
  private final ReservationService reservationService;

  public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  @PostMapping("/resources/{resourceId}/reservations")
  public ResponseEntity<ReservationResponse> reserve(
      @PathVariable UUID resourceId,
      @Parameter(
              name = "Idempotency-Key",
              in = ParameterIn.HEADER,
              description = "Optional retry key; identical requests replay the original response")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idempotencyKey) {
    var created = reservationService.reserve(resourceId);
    return ResponseEntity.created(URI.create("/api/v1/reservations/" + created.id())).body(created);
  }

  @GetMapping("/reservations")
  public ResponseEntity<List<ReservationResponse>> findAll() {
    return ResponseEntity.ok(reservationService.findAll());
  }

  @DeleteMapping("/reservations/{id}")
  public ResponseEntity<Void> cancel(@PathVariable UUID id) {
    reservationService.cancel(id);
    return ResponseEntity.noContent().build();
  }
}
