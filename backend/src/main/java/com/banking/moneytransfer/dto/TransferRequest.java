package com.banking.moneytransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Source account number is required")
        String fromAccountNumber,

        @NotBlank(message = "Destination account number is required")
        String toAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @jakarta.validation.constraints.DecimalMax(value = "10000000.00", message = "Transfer amount cannot exceed 10,000,000.00 Rs")
        BigDecimal amount,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {}
