package com.ricardoporto.lending.loan;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateLoanRequest(@NotNull UUID resourceItemId, Long borrowerId) {}
