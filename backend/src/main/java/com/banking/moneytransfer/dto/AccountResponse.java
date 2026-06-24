package com.banking.moneytransfer.dto;

import com.banking.moneytransfer.domain.AccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        com.banking.moneytransfer.domain.AccountType accountType,
        String firstName,
        String lastName,
        BigDecimal balance,
        AccountStatus status,
        LocalDateTime openedDate,
        LocalDateTime lastUpdated,
        String closureReason
) {}
