package com.banking.moneytransfer.dto;

import com.banking.moneytransfer.domain.AccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String holderName,
        BigDecimal balance,
        AccountStatus status,
        LocalDateTime lastUpdated
) {}
