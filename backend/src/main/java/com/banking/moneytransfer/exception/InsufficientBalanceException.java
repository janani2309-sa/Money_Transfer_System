package com.banking.moneytransfer.exception;

public final class InsufficientBalanceException extends BankingException {
    public InsufficientBalanceException(String message) {
        super(message, "TRX-400");
    }
}
