package com.banking.moneytransfer.dto;

import com.banking.moneytransfer.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
    @NotNull(message = "Account type is required")
    AccountType accountType,

    @NotNull(message = "Initial deposit is required")
    @jakarta.validation.constraints.DecimalMin(value = "1000.00", message = "Initial deposit must be at least 1000.00 Rs")
    @jakarta.validation.constraints.DecimalMax(value = "10000000.00", message = "Initial deposit cannot exceed 10,000,000.00 Rs")
    BigDecimal initialDeposit,

    @NotBlank(message = "Document type is required")
    String documentType,

    @NotBlank(message = "Document number is required")
    String documentNumber
) {}
