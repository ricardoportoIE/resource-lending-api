package com.ricardoporto.lending.user;

public enum Role {
  STUDENT,
  STAFF,
  ADMIN;

  public String authority() {
    return "ROLE_" + name();
  }
}
