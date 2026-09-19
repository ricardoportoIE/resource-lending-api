package com.ricardoporto.lending.auth;

public record AuthTokensResponse(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {
  @Override
  public String toString() {
    return "AuthTokensResponse[accessToken=<redacted>, refreshToken=<redacted>, tokenType="
        + tokenType
        + ", expiresIn="
        + expiresIn
        + "]";
  }
}
