package com.banking.moneytransfer.exception;

public final class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message, "ACC-404");
    }
}
