package com.banking.moneytransfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionLogResponse(
    String id,
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount,
    String status,
    String failureReason,
    String idempotencyKey,
    LocalDateTime createdOn
) {}
