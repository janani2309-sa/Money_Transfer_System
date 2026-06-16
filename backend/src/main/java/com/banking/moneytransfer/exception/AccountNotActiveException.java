package com.banking.moneytransfer.exception;

public final class AccountNotActiveException extends BankingException {
    public AccountNotActiveException(String message) {
        super(message, "ACC-403");
    }
}
