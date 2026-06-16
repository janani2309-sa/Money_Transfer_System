package com.banking.moneytransfer.dto;

public record ErrorResponse(
        String errorCode,
        String message
) {}
