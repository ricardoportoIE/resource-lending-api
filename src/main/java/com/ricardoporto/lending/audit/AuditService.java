package com.ricardoporto.lending.audit;

import com.ricardoporto.lending.shared.security.AuthorizationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditEventRepository repository;
  private final AuthorizationService authorizationService;

  public AuditService(AuditEventRepository repository, AuthorizationService authorizationService) {
    this.repository = repository;
    this.authorizationService = authorizationService;
  }

  public void record(String action, String entityType, Object entityId, String metadata) {
    var event = new AuditEvent();
    event.setId(UUID.randomUUID());
    event.setActor(authorizationService.currentUser());
    event.setAction(action);
    event.setEntityType(entityType);
    event.setEntityId(entityId.toString());
    event.setOccurredAt(Instant.now());
    event.setMetadata(metadata);
    repository.save(event);
  }
}
