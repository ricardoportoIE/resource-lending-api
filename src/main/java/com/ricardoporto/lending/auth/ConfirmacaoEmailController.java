package com.ricardoporto.lending.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfirmacaoEmailController {

  private final EmailConfirmationService emailConfirmationService;

  public ConfirmacaoEmailController(EmailConfirmationService emailConfirmationService) {
    this.emailConfirmationService = emailConfirmationService;
  }

  @GetMapping("/confirmar-email")
  public ResponseEntity<Void> confirmarEmail(@RequestParam String token) {
    emailConfirmationService.confirm(token);
    return ResponseEntity.ok().build();
  }
}
