package com.ricardoporto.lending.user;

import java.util.Set;

public record CurrentUserResponse(
    Long id, String email, String firstName, String lastName, Set<String> roles) {}
