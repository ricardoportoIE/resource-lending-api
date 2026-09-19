package com.ricardoporto.lending.auth;

public record AuthTokensDto(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {
  @Override
  public String toString() {
    return "AuthTokensDto[accessToken=<redacted>, refreshToken=<redacted>, tokenType="
        + tokenType
        + ", expiresIn="
        + expiresIn
        + "]";
  }
}
