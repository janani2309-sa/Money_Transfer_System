package com.banking.moneytransfer.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String transactionId,
        String status,
        String message,
        Long debitedFrom,
        Long creditedTo,
        BigDecimal amount
) {}
