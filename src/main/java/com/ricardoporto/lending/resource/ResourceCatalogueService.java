package com.ricardoporto.lending.resource;

import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceCatalogueService {
  private final ResourceRepository resourceRepository;
  private final ResourceItemRepository itemRepository;

  public ResourceCatalogueService(
      ResourceRepository resourceRepository, ResourceItemRepository itemRepository) {
    this.resourceRepository = resourceRepository;
    this.itemRepository = itemRepository;
  }

  @Transactional(readOnly = true)
  public Page<ResourceResponse> findAll(
      ResourceType type, String category, ResourceItemStatus status, Pageable pageable) {
    return resourceRepository
        .findFiltered(type, normalize(category), status, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public ResourceResponse findById(UUID id) {
    return toResponse(requireResource(id));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResourceResponse create(CreateResourceRequest request) {
    assertIdentifierAvailable(request.identifier());
    var now = Instant.now();
    var resource = new Resource();
    resource.setId(UUID.randomUUID());
    resource.setName(request.name().trim());
    resource.setDescription(request.description());
    resource.setType(request.type());
    resource.setCategory(normalize(request.category()));
    resource.setIdentifier(normalize(request.identifier()));
    resource.setLoanable(request.loanable());
    resource.setCreatedAt(now);
    resource.setUpdatedAt(now);
    return toResponse(resourceRepository.save(resource));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResourceResponse update(UUID id, UpdateResourceRequest request) {
    var resource = requireResource(id);
    if (request.name() != null) resource.setName(request.name().trim());
    if (request.description() != null) resource.setDescription(request.description());
    if (request.type() != null) resource.setType(request.type());
    if (request.category() != null) resource.setCategory(normalize(request.category()));
    if (request.identifier() != null && !request.identifier().equals(resource.getIdentifier())) {
      assertIdentifierAvailable(request.identifier());
      resource.setIdentifier(normalize(request.identifier()));
    }
    if (request.loanable() != null) resource.setLoanable(request.loanable());
    resource.setUpdatedAt(Instant.now());
    return toResponse(resourceRepository.save(resource));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResourceItemResponse addItem(UUID resourceId, CreateResourceItemRequest request) {
    var assetTag = request.assetTag().trim();
    if (itemRepository.existsByAssetTag(assetTag)) {
      throw new ApiException(
          HttpStatus.CONFLICT, "ASSET_TAG_ALREADY_EXISTS", "The asset tag is already in use.");
    }
    var now = Instant.now();
    var item = new ResourceItem();
    item.setId(UUID.randomUUID());
    item.setResource(requireResource(resourceId));
    item.setAssetTag(assetTag);
    item.setStatus(ResourceItemStatus.AVAILABLE);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    return ResourceMapper.toItemResponse(itemRepository.save(item));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResourceItemResponse updateItemStatus(
      UUID itemId, UpdateResourceItemStatusRequest request) {
    var item =
        itemRepository
            .findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource item", itemId));
    item.setStatus(request.status());
    item.setUpdatedAt(Instant.now());
    return ResourceMapper.toItemResponse(itemRepository.save(item));
  }

  private ResourceResponse toResponse(Resource resource) {
    return ResourceMapper.toResponse(
        resource, itemRepository.findAllByResourceIdOrderByAssetTag(resource.getId()));
  }

  private Resource requireResource(UUID id) {
    return resourceRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
  }

  private void assertIdentifierAvailable(String identifier) {
    var normalized = normalize(identifier);
    if (normalized != null && resourceRepository.existsByIdentifier(normalized)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "RESOURCE_IDENTIFIER_ALREADY_EXISTS",
          "The resource identifier is already in use.");
    }
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
