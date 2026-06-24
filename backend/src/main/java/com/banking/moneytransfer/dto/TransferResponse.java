package com.banking.moneytransfer.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String transactionId,
        String status,
        String message,
        String debitedFrom,
        String creditedTo,
        BigDecimal amount
) {}
