package com.ricardoporto.lending.resource;

import java.util.List;

public final class ResourceMapper {
  private ResourceMapper() {}

  public static ResourceResponse toResponse(Resource resource, List<ResourceItem> items) {
    return new ResourceResponse(
        resource.getId(),
        resource.getName(),
        resource.getDescription(),
        resource.getType(),
        resource.getCategory(),
        resource.getIdentifier(),
        resource.isLoanable(),
        resource.getCreatedAt(),
        resource.getUpdatedAt(),
        items.stream().map(ResourceMapper::toItemResponse).toList());
  }

  public static ResourceItemResponse toItemResponse(ResourceItem item) {
    return new ResourceItemResponse(
        item.getId(), item.getAssetTag(), item.getStatus(), item.getVersion(), item.getUpdatedAt());
  }
}
