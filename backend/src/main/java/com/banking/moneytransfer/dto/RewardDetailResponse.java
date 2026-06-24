package com.banking.moneytransfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardDetailResponse(
    Long id,
    String transactionId,
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal transactionAmount,
    int pointsEarned,
    LocalDateTime createdAt
) {}
