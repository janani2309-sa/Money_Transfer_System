package com.banking.moneytransfer.exception;

public abstract sealed class BankingException extends RuntimeException permits
        AccountNotFoundException,
        AccountNotActiveException,
        InsufficientBalanceException,
        DuplicateTransferException {

    private final String errorCode;

    protected BankingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
