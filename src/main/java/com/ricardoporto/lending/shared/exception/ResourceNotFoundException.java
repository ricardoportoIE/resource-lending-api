package com.ricardoporto.lending.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

  public ResourceNotFoundException(String resourceName, Object id) {
    super(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "%s with identifier %s was not found.".formatted(resourceName, id));
  }
}
